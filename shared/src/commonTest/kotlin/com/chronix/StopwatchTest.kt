package com.chronix

import com.chronix.domain.ForceMode
import com.chronix.domain.ForcePlan
import com.chronix.domain.ForceTarget
import com.chronix.domain.StopSubmode
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

    @Test fun forcedTotalsRoundUpNeverDown() {
        val sw = Stopwatch()
        sw.resetAndArm(listOf(45, 96))
        sw.start(0)
        sw.lap(5_480) // real 5,48: las centésimas 45 ya pasaron -> segundo siguiente, 6,45
        sw.lap(11_230) // real 11,23: 96 aún no ha llegado -> mismo segundo, 11,96
        assertEquals(645, sw.laps[0].totalCs)
        assertEquals(1196, sw.laps[1].totalCs)
        assertEquals(1196 - 645, sw.laps[1].lapCs)
        assertEquals(sw.laps[0].totalCs + sw.laps[1].lapCs, sw.laps[1].totalCs)
        assertFalse(sw.forcingArmed)
    }

    @Test fun lapPastTheForcedPointJumpsToNextSecond() {
        // Caso del usuario: vuelta en el segundo 1 con 01 forzado -> 2,01 (antes salía 1,01)
        val sw = Stopwatch()
        sw.resetAndArm(listOf(1))
        sw.start(0)
        sw.lap(1_600)
        assertEquals(201, sw.laps[0].totalCs)
    }

    @Test fun lapBeforeTheForcedPointStaysInSameSecond() {
        val sw = Stopwatch()
        sw.resetAndArm(listOf(50))
        sw.start(0)
        sw.lap(1_200)
        assertEquals(150, sw.laps[0].totalCs)
    }

    @Test fun forcedTotalIsNeverBelowRealTime() {
        for (real in listOf(0L, 1L, 99L, 100L, 101L, 555L, 1234L, 99_999L)) {
            for (f in listOf(0, 1, 22, 50, 99)) {
                val t = Stopwatch.forceTotal(real, -1, f) // sin límite por vuelta anterior: fórmula pura
                assertTrue(t >= real, "real=$real f=$f t=$t")
                assertTrue(t - real < 100, "salta menos de 1 s: real=$real f=$f t=$t")
                assertEquals(f.toLong(), t % 100)
            }
        }
    }

    @Test fun lapTargetForcesTheLapTimeCentiseconds() {
        // iPhone: solo se ve el tiempo de vuelta. 01, 22, 33, 04 deben verse tal cual en cada vuelta.
        val sw = Stopwatch()
        sw.resetAndArm(listOf(1, 22, 33, 4), ForceTarget.LAP)
        sw.start(0)
        val realTotals = listOf(1_600L, 3_100L, 5_800L, 6_500L)
        realTotals.forEach { sw.lap(it) }
        assertEquals(listOf(1L, 22L, 33L, 4L), sw.laps.map { it.lapCs % 100 })
        // el total sale por suma y nunca va por detrás del tiempo real
        var acc = 0L
        sw.laps.forEachIndexed { i, lap ->
            acc += lap.lapCs
            assertEquals(acc, lap.totalCs)
            assertTrue(lap.lapCs > 0)
            assertTrue(lap.totalCs >= realTotals[i] / 10, "vuelta ${i + 1}")
        }
    }

    @Test fun lapTargetNeverRoundsDown() {
        val sw = Stopwatch()
        sw.resetAndArm(listOf(1), ForceTarget.LAP)
        sw.start(0)
        sw.lap(1_600) // vuelta real 1,60 -> 2,01
        assertEquals(201, sw.laps[0].lapCs)
        assertEquals(4L, Stopwatch.forceLap(-6, 4)) // vuelta real negativa: primera duración positiva con 04
    }

    @Test fun forceLapAlwaysPositiveAndAtLeastRealLap() {
        for (real in listOf(-250L, -6L, 0L, 1L, 99L, 100L, 160L, 5_000L)) {
            for (f in listOf(0, 1, 22, 99)) {
                val l = Stopwatch.forceLap(real, f)
                assertTrue(l > 0, "real=$real f=$f l=$l")
                assertTrue(l >= real, "real=$real f=$f l=$l")
                assertEquals(f.toLong(), l % 100)
            }
        }
    }

    @Test fun forcingEndsAndLaterLapsAreNormal() {
        val sw = Stopwatch()
        sw.resetAndArm(listOf(10))
        sw.start(0)
        sw.lap(2_000) // real 2,00 -> 2,10 (mismo segundo)
        sw.lap(3_333)
        assertEquals(210, sw.laps[0].totalCs)
        assertEquals(333, sw.laps[1].totalCs)
    }

    @Test fun forcedLapNeverZeroOrNegative() {
        val sw = Stopwatch()
        sw.resetAndArm(listOf(90, 10))
        sw.start(0)
        sw.lap(120) // real 0,12 -> 0,90
        sw.lap(200) // real 0,20; 0,10 quedaría por detrás: sube hasta superar 0,90 -> 1,10
        assertTrue(sw.laps.all { it.lapCs > 0 })
        assertEquals(90, sw.laps[0].totalCs % 100)
        assertEquals(10, sw.laps[1].totalCs % 100)
    }

    // ---- Modo cronómetro: el forzaje actúa al parar (una vez), no en las vueltas

    @Test fun stopModeCentisForcesTheBigCounterOnStop() {
        val sw = Stopwatch()
        sw.resetAndArm(ForcePlan(ForceMode.STOPWATCH, stopSubmode = StopSubmode.CENTIS, stopCentis = 45))
        sw.start(0)
        sw.stop(5_480) // real 5,48: las centésimas 45 ya pasaron -> 6,45 (los segundos nunca retroceden, nunca 5,45)
        assertEquals(645, sw.elapsedCs(99_999))
        assertEquals(Phase.PAUSED, sw.phase)
        assertFalse(sw.forcingArmed)
    }

    @Test fun stopModeCentisStaysInSameSecondWhenNotPassed() {
        val sw = Stopwatch()
        sw.resetAndArm(ForcePlan(ForceMode.STOPWATCH, stopCentis = 90))
        sw.start(0)
        sw.stop(5_480)
        assertEquals(590, sw.elapsedCs(0))
    }

    @Test fun stopModeDoesNotForceLaps() {
        val sw = Stopwatch()
        sw.resetAndArm(ForcePlan(ForceMode.STOPWATCH, stopCentis = 45))
        sw.start(0)
        sw.lap(1_230)
        sw.lap(2_870)
        assertEquals(listOf(123L, 287L), sw.laps.map { it.totalCs })
        assertTrue(sw.forcingArmed) // sigue armado hasta que se pare
    }

    @Test fun stopModeFullCounterForcesMinutesSecondsAndCentis() {
        val sw = Stopwatch()
        // 01:10,34 = 1 min 10 s 34 cs = 7034 cs
        sw.resetAndArm(ForcePlan(ForceMode.STOPWATCH, stopSubmode = StopSubmode.FULL, stopFullCs = 7_034L))
        sw.start(0)
        sw.stop(65_000) // real 1:05,00 -> exactamente 01:10,34
        assertEquals(7_034, sw.elapsedCs(0))
        assertEquals("01:10,34", formatCs(sw.elapsedCs(0)))
    }

    @Test fun stopModeFullCounterSubstitutesAsIsEvenWhenGoingBackwards() {
        val sw = Stopwatch()
        // Se fuerzan los tres (minutos, segundos y centésimas): se sustituye tal cual aunque el real ya haya pasado
        sw.resetAndArm(ForcePlan(ForceMode.STOPWATCH, stopSubmode = StopSubmode.FULL, stopFullCs = 510L))
        sw.start(0)
        sw.stop(8_000) // real 8,00; configurado 00:05,10 -> 00:05,10
        assertEquals(510, sw.elapsedCs(0))
        assertEquals("00:05,10", formatCs(sw.elapsedCs(0)))
    }

    @Test fun stopModeKeepsTheActiveLapConsistentWithLapsAndCounter() {
        val sw = Stopwatch()
        sw.resetAndArm(ForcePlan(ForceMode.STOPWATCH, stopSubmode = StopSubmode.FULL, stopFullCs = 1_234L))
        sw.start(0)
        sw.lap(3_000)
        sw.lap(5_000)
        sw.stop(9_000) // el contador queda en 12,34
        val counter = sw.elapsedCs(0)
        assertEquals(1_234, counter)
        // vuelta en curso = contador - suma de las vueltas hechas
        assertEquals(counter - sw.laps.sumOf { it.lapCs }, sw.currentLapCs(0))
        assertEquals(1_234 - 500, sw.currentLapCs(0))
    }

    @Test fun stopModeAppliesOnlyToTheFirstStopAndTheCounterKeepsRunningOnTheSameLap() {
        val sw = Stopwatch()
        sw.resetAndArm(ForcePlan(ForceMode.STOPWATCH, stopCentis = 10))
        sw.start(0)
        sw.stop(2_000) // 2,00 -> 2,10
        assertEquals(210, sw.elapsedCs(0))
        assertFalse(sw.forcingArmed)
        sw.start(10_000) // reanudar: sigue en la misma vuelta, sin vueltas nuevas ni reset
        sw.stop(11_500) // segunda parada, ya sin forzaje: 2,10 + 1,50
        assertEquals(360, sw.elapsedCs(0))
        assertTrue(sw.laps.isEmpty())
    }

    @Test fun stopModeFullCounterBelowTheLastLapKeepsTheActiveLapAtZero() {
        val sw = Stopwatch()
        sw.resetAndArm(ForcePlan(ForceMode.STOPWATCH, stopSubmode = StopSubmode.FULL, stopFullCs = 100L))
        sw.start(0)
        sw.lap(3_000) // total 3,00
        sw.stop(3_500) // se sustituye por 1,00 (menor que la vuelta): la vuelta en curso no puede ser negativa
        assertEquals(100, sw.elapsedCs(0))
        assertEquals(0, sw.currentLapCs(0))
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
