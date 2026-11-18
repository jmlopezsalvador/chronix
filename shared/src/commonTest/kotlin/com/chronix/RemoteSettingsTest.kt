package com.chronix

import com.chronix.domain.ForceMode
import com.chronix.domain.ForceTarget
import com.chronix.domain.StopSubmode
import com.chronix.ui.AppSettings
import com.chronix.ui.RemoteKeys
import com.chronix.ui.RemoteOverrides
import com.chronix.ui.STOP_FULL_CS_MAX
import com.chronix.ui.forcePlan
import com.chronix.ui.parseRemoteOverrides
import com.chronix.ui.withRemote
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.test.Test
import kotlin.test.assertEquals

/** Capa de ajustes remotos de Magic Friends: lista blanca, solapado `remoto ?: local` y validación. */
class RemoteSettingsTest {
    private val local = AppSettings(
        themeId = "samsung",
        forcedCs = listOf(11, 22),
        forceTarget = ForceTarget.LAP,
        forceMode = ForceMode.LAPS,
        stopSubmode = StopSubmode.CENTIS,
        stopCentis = 5,
        stopFullCs = 100L,
    )

    private fun effective(remote: JsonObject) = local.withRemote(parseRemoteOverrides(remote))

    @Test fun emptyRemoteChangesNothing() {
        assertEquals(RemoteOverrides(), parseRemoteOverrides(JsonObject(emptyMap())))
        assertEquals(local, effective(JsonObject(emptyMap())))
    }

    @Test fun remoteOverridesOnlyTheKeysItSets() {
        val eff = effective(buildJsonObject {
            put("forceMode", "STOPWATCH")
            put("stopCentis", 45)
        })
        assertEquals(ForceMode.STOPWATCH, eff.forceMode)
        assertEquals(45, eff.stopCentis)
        // El resto sigue siendo local
        assertEquals(local.forcedCs, eff.forcedCs)
        assertEquals(local.forceTarget, eff.forceTarget)
        assertEquals(local.stopSubmode, eff.stopSubmode)
        assertEquals(local.stopFullCs, eff.stopFullCs)
    }

    @Test fun everyWhitelistedKeyCanBeOverridden() {
        val eff = effective(buildJsonObject {
            put("forceMode", "STOPWATCH")
            put("forceTarget", "TOTAL")
            put("stopSubmode", "FULL")
            putJsonArray("forcedCs") { add(1); add(2); add(3) }
            put("stopCentis", 77)
            put("stopFullCs", 7_034)
        })
        assertEquals(ForceMode.STOPWATCH, eff.forceMode)
        assertEquals(ForceTarget.TOTAL, eff.forceTarget)
        assertEquals(StopSubmode.FULL, eff.stopSubmode)
        assertEquals(listOf(1, 2, 3), eff.forcedCs)
        assertEquals(77, eff.stopCentis)
        assertEquals(7_034L, eff.stopFullCs)
        assertEquals(6, RemoteKeys.all.size)
    }

    @Test fun keysOutsideTheWhitelistAreNeverApplied() {
        val remote = buildJsonObject {
            put("themeId", "huawei")
            put("iconId", "samsung")
            put("nameId", "reloj")
            put("showTotals", false)
            put("secretHoldMs", 5000)
            put("showGuide", false)
            put("magicFriendsEnabled", true)
        }
        assertEquals(RemoteOverrides(), parseRemoteOverrides(remote))
        assertEquals(local, effective(remote))
    }

    @Test fun numbersOutOfRangeAreClampedLikeLocalSettings() {
        val eff = effective(buildJsonObject {
            put("stopCentis", 150)
            put("stopFullCs", 99_999_999L)
            putJsonArray("forcedCs") { add(150); add(-3); add(45) }
        })
        assertEquals(99, eff.stopCentis)
        assertEquals(STOP_FULL_CS_MAX, eff.stopFullCs)
        assertEquals(listOf(99, 0, 45), eff.forcedCs)
        assertEquals(0, effective(buildJsonObject { put("stopCentis", -8) }).stopCentis)
        assertEquals(0L, effective(buildJsonObject { put("stopFullCs", -1) }).stopFullCs)
    }

    @Test fun wrongTypesAndUnknownValuesAreIgnoredSoTheLocalOneWins() {
        val remote = buildJsonObject {
            put("forceMode", "NO_EXISTE")
            put("forceTarget", 3)
            put("stopSubmode", true)
            put("stopCentis", "45") // un número como texto no vale
            put("stopFullCs", "abc")
            put("forcedCs", "1,2,3") // debe ser un array
        }
        assertEquals(RemoteOverrides(), parseRemoteOverrides(remote))
        assertEquals(local, effective(remote))
    }

    @Test fun forcedCsWithAnInvalidElementIsIgnoredAsAWholeSoLapPositionsNeverShift() {
        val eff = effective(buildJsonObject {
            putJsonArray("forcedCs") { add(10); add("x"); add(30) }
        })
        assertEquals(local.forcedCs, eff.forcedCs)
    }

    @Test fun anEmptyForcedListIsValidAndDisablesLapForcing() {
        val eff = effective(buildJsonObject { putJsonArray("forcedCs") {} })
        assertEquals(emptyList(), eff.forcedCs)
    }

    @Test fun forcePlanWithoutRemoteIsTheLocalPlan() {
        val plan = local.forcePlan()
        assertEquals(ForceMode.LAPS, plan.mode)
        assertEquals(listOf(11, 22), plan.lapCs)
        assertEquals(5, plan.stopCentis)
        val same = local.forcePlan(null)
        assertEquals(plan.lapCs, same.lapCs)
        assertEquals(plan.mode, same.mode)
    }

    @Test fun forcePlanTakesTheRemoteValues() {
        val plan = local.forcePlan(parseRemoteOverrides(buildJsonObject {
            put("forceMode", "STOPWATCH")
            put("stopSubmode", "FULL")
            put("stopFullCs", 7_034)
            putJsonArray("forcedCs") { add(90) }
        }))
        assertEquals(ForceMode.STOPWATCH, plan.mode)
        assertEquals(StopSubmode.FULL, plan.stopSubmode)
        assertEquals(7_034L, plan.stopFullCs)
        assertEquals(listOf(90), plan.lapCs)
    }

    @Test fun forcePlanStillFallsBackToLapWhenTotalsAreHiddenLocally() {
        // El mago decide qué se ve: aunque el ayudante pida forzar el total, sin totales visibles se fuerza la vuelta
        val hidden = AppSettings(themeId = "samsung", showTotals = false)
        val plan = hidden.forcePlan(parseRemoteOverrides(buildJsonObject { put("forceTarget", "TOTAL") }))
        assertEquals(ForceTarget.LAP, plan.lapTarget)
        val visible = AppSettings(themeId = "samsung").forcePlan(parseRemoteOverrides(buildJsonObject { put("forceTarget", "TOTAL") }))
        assertEquals(ForceTarget.TOTAL, visible.lapTarget)
    }
}
