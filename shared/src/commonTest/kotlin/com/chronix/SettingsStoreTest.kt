package com.chronix

import com.chronix.domain.ForceMode
import com.chronix.domain.ForceTarget
import com.chronix.domain.StopSubmode
import com.chronix.ui.AppSettings
import com.chronix.ui.KeyValueStore
import com.chronix.ui.SettingsStore
import com.chronix.ui.forcePlan
import com.chronix.ui.totalsVisible
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsStoreTest {
    private class MemoryStore : KeyValueStore {
        val m = HashMap<String, Any>()
        override fun getString(key: String) = m[key] as String?
        override fun getBoolean(key: String, default: Boolean) = m[key] as Boolean? ?: default
        override fun getLong(key: String, default: Long) = m[key] as Long? ?: default
        override fun putString(key: String, value: String) { m[key] = value }
        override fun putBoolean(key: String, value: Boolean) { m[key] = value }
        override fun putLong(key: String, value: Long) { m[key] = value }
    }

    @Test fun defaultsHaveFourForcedLapsAndOneAndAHalfSeconds() {
        val s = SettingsStore(MemoryStore()).load()
        assertEquals(4, s.forcedCs.size)
        assertEquals(1500L, s.secretHoldMs)
    }

    @Test fun savesAndLoadsSettings() {
        val store = SettingsStore(MemoryStore())
        store.save(AppSettings(secretHoldMs = 2000L, forcedCs = listOf(45, 96, 7, 0, 12)))
        val s = store.load()
        assertEquals(2000L, s.secretHoldMs)
        assertEquals(listOf(45, 96, 7, 0, 12), s.forcedCs)
    }

    @Test fun forceTargetDefaultsToLapAndSurvivesRoundTrip() {
        val store = SettingsStore(MemoryStore())
        assertEquals(ForceTarget.LAP, store.load().forceTarget)
        store.save(AppSettings(forceTarget = ForceTarget.TOTAL))
        assertEquals(ForceTarget.TOTAL, store.load().forceTarget)
        store.save(AppSettings(forceTarget = ForceTarget.LAP))
        assertEquals(ForceTarget.LAP, store.load().forceTarget)
    }

    @Test fun guideIsActiveByDefaultAndIconNameSurviveRoundTrip() {
        val store = SettingsStore(MemoryStore())
        val d = store.load()
        assertEquals(true, d.showGuide)
        assertEquals("google", d.iconId)
        assertEquals("chronix", d.nameId)
        store.save(AppSettings(showGuide = false, iconId = "samsung", nameId = "reloj"))
        val s = store.load()
        assertEquals(false, s.showGuide)
        assertEquals("samsung", s.iconId)
        assertEquals("reloj", s.nameId)
    }

    @Test fun unknownIconOrNameFallsBackToDefault() {
        val mem = MemoryStore()
        mem.putString("iconId", "no_existe")
        mem.putString("nameId", "tampoco")
        val s = SettingsStore(mem).load()
        assertEquals("google", s.iconId)
        assertEquals("chronix", s.nameId)
    }

    @Test fun stopModeSettingsDefaultsAndRoundTrip() {
        val store = SettingsStore(MemoryStore())
        val d = store.load()
        assertEquals(ForceMode.LAPS, d.forceMode)
        assertEquals(StopSubmode.CENTIS, d.stopSubmode)
        assertEquals(true, d.showTotals)
        assertEquals(0, d.stopCentis)
        assertEquals(0L, d.stopFullCs)
        store.save(AppSettings(
            forceMode = ForceMode.STOPWATCH, stopSubmode = StopSubmode.FULL, showTotals = false,
            stopCentis = 45, stopFullCs = 7_034L,
        ))
        val s = store.load()
        assertEquals(ForceMode.STOPWATCH, s.forceMode)
        assertEquals(StopSubmode.FULL, s.stopSubmode)
        assertEquals(false, s.showTotals)
        assertEquals(45, s.stopCentis)
        assertEquals(7_034L, s.stopFullCs)
    }

    @Test fun totalsAreVisibleOnlyIfSettingOnAndThemeHasThem() {
        assertEquals(true, AppSettings(themeId = "samsung").totalsVisible())
        assertEquals(false, AppSettings(themeId = "samsung", showTotals = false).totalsVisible())
        assertEquals(false, AppSettings(themeId = "iphone").totalsVisible()) // el iPhone no tiene columna de totales
    }

    @Test fun forcingTotalFallsBackToLapWhenTotalsAreHidden() {
        val visible = AppSettings(themeId = "samsung", forceTarget = ForceTarget.TOTAL).forcePlan()
        assertEquals(ForceTarget.TOTAL, visible.lapTarget)
        val hidden = AppSettings(themeId = "samsung", showTotals = false, forceTarget = ForceTarget.TOTAL).forcePlan()
        assertEquals(ForceTarget.LAP, hidden.lapTarget)
        val iphone = AppSettings(themeId = "iphone", forceTarget = ForceTarget.TOTAL).forcePlan()
        assertEquals(ForceTarget.LAP, iphone.lapTarget)
    }

    @Test fun emptyForcedListSurvivesRoundTrip() {
        val store = SettingsStore(MemoryStore())
        store.save(AppSettings(forcedCs = emptyList()))
        assertEquals(emptyList(), store.load().forcedCs)
    }
}
