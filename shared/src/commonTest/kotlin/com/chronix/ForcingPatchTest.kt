package com.chronix

import com.chronix.domain.ForceMode
import com.chronix.domain.ForceTarget
import com.chronix.domain.StopSubmode
import com.chronix.ui.AppSettings
import com.chronix.ui.ForcingValues
import com.chronix.ui.RemoteKeys
import com.chronix.ui.RemoteOverrides
import com.chronix.ui.STOP_FULL_CS_MAX
import com.chronix.ui.forcePlan
import com.chronix.ui.forcingValues
import com.chronix.ui.hasFullForcingPlan
import com.chronix.ui.parseRemoteOverrides
import com.chronix.ui.releasePatch
import com.chronix.ui.toForcingValues
import com.chronix.ui.toPatch
import com.chronix.ui.withForcing
import com.chronix.ui.withRemote
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Fase 2 (Chronix como ayudante): lo que el ayudante manda tiene que ser exactamente lo que el móvil del mago acepta
 * (`parseRemoteOverrides`), y el editor de forzaje comparte modelo con los ajustes locales.
 */
class ForcingPatchTest {
    private val plan = ForcingValues(
        forceMode = ForceMode.STOPWATCH,
        forceTarget = ForceTarget.TOTAL,
        forcedCs = listOf(4, 25, 99),
        stopSubmode = StopSubmode.FULL,
        stopCentis = 45,
        stopFullCs = 7_034L,
    )

    @Test fun theHelperPatchIsExactlyWhatTheMagoParses() {
        val parsed = parseRemoteOverrides(plan.toPatch())
        assertEquals(
            RemoteOverrides(
                forceMode = ForceMode.STOPWATCH,
                forceTarget = ForceTarget.TOTAL,
                stopSubmode = StopSubmode.FULL,
                forcedCs = listOf(4, 25, 99),
                stopCentis = 45,
                stopFullCs = 7_034L,
            ),
            parsed,
        )
        // Y, aplicado sobre unos ajustes locales cualesquiera, el forzaje efectivo es el del ayudante
        val local = AppSettings(forceMode = ForceMode.LAPS, forcedCs = listOf(1, 2), stopCentis = 9, stopFullCs = 5L)
        assertEquals(plan, local.withRemote(parsed).forcingValues())
    }

    @Test fun thePatchAlwaysCarriesTheSixKeys() {
        assertEquals(RemoteKeys.all, ForcingValues().toPatch().keys)
        assertEquals(RemoteKeys.all, plan.toPatch().keys)
        assertTrue(plan.toPatch().hasFullForcingPlan())
    }

    @Test fun theWireFormatIsTheDocumentedOne() {
        val patch = plan.toPatch()
        assertEquals(JsonPrimitive("STOPWATCH"), patch["forceMode"])
        assertEquals(JsonPrimitive("TOTAL"), patch["forceTarget"])
        assertEquals(JsonPrimitive("FULL"), patch["stopSubmode"])
        assertEquals(JsonPrimitive(45), patch["stopCentis"])
        assertEquals(JsonPrimitive(7_034L), patch["stopFullCs"])
        assertEquals(JsonArray(listOf(JsonPrimitive(4), JsonPrimitive(25), JsonPrimitive(99))), patch["forcedCs"])
    }

    @Test fun outOfRangeValuesAreClampedBeforeSending() {
        val patch = ForcingValues(forcedCs = listOf(150, -3), stopCentis = 200, stopFullCs = 99_999_999L).toPatch()
        val parsed = parseRemoteOverrides(patch)
        assertEquals(listOf(99, 0), parsed.forcedCs)
        assertEquals(99, parsed.stopCentis)
        assertEquals(STOP_FULL_CS_MAX, parsed.stopFullCs)
    }

    @Test fun anEmptyLapListIsSentAsAnEmptyArrayAndMeansNoLapForcing() {
        val patch = ForcingValues(forcedCs = emptyList()).toPatch()
        assertEquals(JsonArray(emptyList()), patch["forcedCs"])
        val local = AppSettings(forcedCs = listOf(11, 22))
        assertEquals(emptyList(), local.withRemote(parseRemoteOverrides(patch)).forcedCs)
    }

    @Test fun releasingTheControlNullsEveryKey() {
        val release = releasePatch()
        assertEquals(RemoteKeys.all, release.keys)
        assertTrue(release.values.all { it is JsonNull })
        assertFalse(release.hasFullForcingPlan())
    }

    @Test fun hasFullForcingPlanNeedsAllSixKeys() {
        assertFalse(JsonObject(emptyMap()).hasFullForcingPlan())
        val partial = buildJsonObject { put("stopCentis", 45); put("forceMode", "LAPS") }
        assertFalse(partial.hasFullForcingPlan())
        assertTrue(plan.toPatch().hasFullForcingPlan())
    }

    @Test fun remoteValuesOverTheDefaultsForMissingKeys() {
        val partial = parseRemoteOverrides(buildJsonObject { put("stopCentis", 45) })
        val values = partial.toForcingValues()
        assertEquals(45, values.stopCentis)
        assertEquals(ForcingValues().copy(stopCentis = 45), values)
        // Con otros valores de partida
        val base = ForcingValues(forceMode = ForceMode.STOPWATCH, forcedCs = listOf(7))
        assertEquals(base.copy(stopCentis = 45), partial.toForcingValues(base))
    }

    @Test fun forcingValuesDefaultsAreTheSettingsDefaults() {
        assertEquals(ForcingValues(), AppSettings().forcingValues())
    }

    @Test fun settingsAndForcingValuesRoundTripWithoutTouchingTheRest() {
        val settings = AppSettings(themeId = "oneplus", secretHoldMs = 2000L, iconId = "samsung", showTotals = false, magicFriendsEnabled = true)
        val back = settings.withForcing(plan)
        assertEquals(plan, back.forcingValues())
        // Lo que no es forzaje queda igual
        assertEquals(settings.copy(
            forceMode = plan.forceMode, forceTarget = plan.forceTarget, forcedCs = plan.forcedCs,
            stopSubmode = plan.stopSubmode, stopCentis = plan.stopCentis, stopFullCs = plan.stopFullCs,
        ), back)
    }

    @Test fun theFullPlanDrivesTheForcePlanOfTheMagoPhone() {
        val local = AppSettings(themeId = "samsung")
        val forcePlan = local.forcePlan(parseRemoteOverrides(plan.toPatch()))
        assertEquals(ForceMode.STOPWATCH, forcePlan.mode)
        assertEquals(StopSubmode.FULL, forcePlan.stopSubmode)
        assertEquals(7_034L, forcePlan.stopFullCs)
        assertEquals(listOf(4, 25, 99), forcePlan.lapCs)
        assertEquals(ForceTarget.TOTAL, forcePlan.lapTarget) // samsung enseña totales
    }
}
