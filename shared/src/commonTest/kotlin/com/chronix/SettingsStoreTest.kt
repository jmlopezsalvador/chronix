package com.chronix

import com.chronix.domain.ForceTarget
import com.chronix.ui.AppSettings
import com.chronix.ui.KeyValueStore
import com.chronix.ui.SettingsStore
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

    @Test fun emptyForcedListSurvivesRoundTrip() {
        val store = SettingsStore(MemoryStore())
        store.save(AppSettings(forcedCs = emptyList()))
        assertEquals(emptyList(), store.load().forcedCs)
    }
}
