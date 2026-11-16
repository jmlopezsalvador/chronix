package com.chronix

import com.chronix.domain.Lap
import com.chronix.domain.LapMark
import com.chronix.domain.Phase
import com.chronix.domain.Stopwatch
import com.chronix.domain.formatCs
import com.chronix.domain.lapMarks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StopwatchTest {
    @Test fun formatsTime() {
        assertEquals("00:00,00", formatCs(0))
        assertEquals("00:19,80", formatCs(1980))
        assertEquals("01:05,07", formatCs(6507))
        assertEquals("59:59,99", formatCs(359_999))
        assertEquals("1:00:00,00", formatCs(360_000))
    }

    @Test fun runsPausesAndResumesWithoutDrift() {
        val sw = Stopwatch()
        sw.start(1000)
        assertEquals(500, sw.elapsedMs(1500))
        sw.stop(2000)
        assertEquals(Phase.PAUSED, sw.phase)
        assertEquals(1000, sw.elapsedMs(9999)) // en pausa no avanza
        sw.start(10_000)
        assertEquals(1500, sw.elapsedMs(10_500))
    }

    @Test fun normalLapsUseRealTimes() {
        val sw = Stopwatch()
        sw.start(0)
        sw.lap(5480)
        sw.lap(6980)
        assertEquals(listOf(Lap(1, 548, 548), Lap(2, 150, 698)), sw.laps)
        assertEquals(30, sw.currentLapCs(7280))
    }

    @Test fun lapIgnoredWhenNotRunning() {
        val sw = Stopwatch()
        sw.lap(100)
        assertTrue(sw.laps.isEmpty())
    }

    @Test fun forcedLapsFixCentisecondsAndStayCoherent() {
        val sw = Stopwatch()
        sw.resetAndArm(listOf(45, 96))
        sw.start(0)
        sw.lap(5_480) // real 5,48 -> 5,45
        sw.lap(11_230) // real 11,23 -> 10,96
        assertEquals(545, sw.laps[0].totalCs)
        assertEquals(1096, sw.laps[1].totalCs)
        assertEquals(1096 - 545, sw.laps[1].lapCs)
        assertEquals(sw.laps[0].totalCs + sw.laps[1].lapCs, sw.laps[1].totalCs)
        assertFalse(sw.forcingArmed)
    }

    @Test fun forcingEndsAndLaterLapsAreNormal() {
        val sw = Stopwatch()
        sw.resetAndArm(listOf(10))
        sw.start(0)
        sw.lap(2_000)
        sw.lap(3_333)
        assertEquals(110, sw.laps[0].totalCs)
        assertEquals(333, sw.laps[1].totalCs)
    }

    @Test fun forcedLapNeverZeroOrNegative() {
        val sw = Stopwatch()
        sw.resetAndArm(listOf(90, 10))
        sw.start(0)
        sw.lap(120) // real 0,12 -> 0,90 imposible (<=real) -> -0,10 -> sube a 0,90
        sw.lap(200) // real 0,20 -> debe superar 0,90 -> 1,10
        assertTrue(sw.laps.all { it.lapCs > 0 })
        assertEquals(90, sw.laps[0].totalCs % 100)
        assertEquals(10, sw.laps[1].totalCs % 100)
    }

    @Test fun plainResetDisarmsForcing() {
        val sw = Stopwatch()
        sw.resetAndArm(listOf(45))
        assertTrue(sw.forcingArmed)
        sw.reset()
        assertFalse(sw.forcingArmed)
        sw.start(0)
        sw.lap(1_234)
        assertEquals(123, sw.laps[0].totalCs)
    }

    @Test fun resetClearsEverything() {
        val sw = Stopwatch()
        sw.start(0)
        sw.lap(500)
        sw.reset()
        assertEquals(Phase.IDLE, sw.phase)
        assertTrue(sw.laps.isEmpty())
        assertEquals(0, sw.elapsedMs(10_000))
    }

    @Test fun lapColorsSkipFirstLap() {
        // vuelta 1 es la más larga pero no se colorea
        val laps = listOf(Lap(1, 900, 900), Lap(2, 100, 1000), Lap(3, 300, 1300), Lap(4, 200, 1500))
        assertEquals(mapOf(2 to LapMark.BEST, 3 to LapMark.WORST), lapMarks(laps))
        assertTrue(lapMarks(laps.take(2)).isEmpty())
        assertTrue(lapMarks(laps.take(1)).isEmpty())
    }
}
