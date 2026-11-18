package com.chronix

import com.chronix.domain.ForceMode
import com.chronix.domain.ForcePlan
import com.chronix.domain.ForceTarget
import com.chronix.domain.Stopwatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Cambio en vivo (Magic Friends): el plan se pide al origen en cada vuelta y en cada Parar, no al armar, así que un
 * cambio remoto hecho después de armar vale hasta el último instante.
 */
class StopwatchLiveTest {
    private fun lapsPlan(vararg cs: Int) = ForcePlan(ForceMode.LAPS, lapCs = cs.toList(), lapTarget = ForceTarget.TOTAL)

    @Test fun lapPlanChangedAfterArmingAppliesFromTheNextLap() {
        var plan = lapsPlan(10, 20)
        val sw = Stopwatch()
        sw.resetAndArm { plan }
        sw.start(0)
        sw.lap(1_050) // real 1,05 -> forzado 1,10
        plan = lapsPlan(10, 77) // el ayudante cambia la vuelta 2 después de armar y de hacer la 1
        sw.lap(3_000) // real 3,00 -> forzado 3,77
        assertEquals(110, sw.laps[0].totalCs)
        assertEquals(377, sw.laps[1].totalCs)
    }

    @Test fun stopValueChangedAfterStartingAndBeforeStoppingIsTheOneApplied() {
        var plan = ForcePlan(ForceMode.STOPWATCH, stopCentis = 10)
        val sw = Stopwatch()
        sw.resetAndArm { plan }
        sw.start(0)
        plan = ForcePlan(ForceMode.STOPWATCH, stopCentis = 90) // último instante antes de Parar
        sw.stop(5_480)
        assertEquals(590, sw.elapsedCs(0))
    }

    @Test fun modeSwitchedAfterArmingIsRespected() {
        var plan = lapsPlan(45)
        val sw = Stopwatch()
        sw.resetAndArm { plan }
        sw.start(0)
        plan = ForcePlan(ForceMode.STOPWATCH, stopCentis = 45) // de vueltas a cronómetro
        sw.lap(1_230) // ya no se fuerzan las vueltas
        assertEquals(123, sw.laps[0].totalCs)
        sw.stop(5_480) // sí se fuerza al parar: 6,45
        assertEquals(645, sw.elapsedCs(0))
    }

    @Test fun lapNUsesPositionNEvenIfEarlierLapsWereNormal() {
        var plan = ForcePlan(ForceMode.STOPWATCH, stopCentis = 45)
        val sw = Stopwatch()
        sw.resetAndArm { plan }
        sw.start(0)
        sw.lap(1_230) // normales: 1,23 y 2,87
        sw.lap(2_870)
        plan = lapsPlan(11, 22, 33) // cambia a vueltas con el forzaje aún sin consumir
        sw.lap(4_000) // es la vuelta 3: usa la posición 3 (33), no la 1
        assertEquals(listOf(123L, 287L, 433L), sw.laps.map { it.totalCs })
    }

    @Test fun stopForcingActsOnceAndDisarmsForGood() {
        var plan = ForcePlan(ForceMode.STOPWATCH, stopCentis = 45)
        val sw = Stopwatch()
        sw.resetAndArm { plan }
        sw.start(0)
        sw.stop(5_480)
        assertEquals(645, sw.elapsedCs(0))
        assertFalse(sw.forcingArmed)
        plan = lapsPlan(11, 22) // el ayudante cambia a vueltas cuando el forzaje de Parar ya actuó
        sw.start(10_000)
        sw.lap(11_000) // sigue siendo un cronómetro normal: 6,45 + 1,00 = 7,45
        assertEquals(745, sw.laps[0].totalCs)
    }

    @Test fun aFailingSourceFallsBackToTheLastPlanRead() {
        var fail = false
        val sw = Stopwatch()
        sw.resetAndArm { if (fail) error("sin red") else lapsPlan(10, 20) }
        sw.start(0)
        sw.lap(1_050)
        fail = true // el origen falla: no debe romper el efecto
        sw.lap(3_000)
        assertEquals(110, sw.laps[0].totalCs)
        assertEquals(320, sw.laps[1].totalCs)
    }

    @Test fun resetDisarmsAndStopsConsultingTheSource() {
        var calls = 0
        val sw = Stopwatch()
        sw.resetAndArm { calls++; lapsPlan(10) }
        assertTrue(sw.forcingArmed)
        sw.reset()
        val after = calls
        sw.start(0)
        sw.lap(1_000)
        sw.stop(2_000)
        assertEquals(after, calls)
        assertFalse(sw.forcingArmed)
        assertEquals(100, sw.laps[0].totalCs) // vuelta normal
    }

    @Test fun anArmedLiveListThatGrowsLaterKeepsForcing() {
        var plan = lapsPlan(10)
        val sw = Stopwatch()
        sw.resetAndArm { plan }
        sw.start(0)
        sw.lap(1_050)
        assertFalse(sw.forcingArmed) // la lista de una posición ya se consumió
        plan = lapsPlan(10, 50) // el ayudante añade la vuelta 2
        assertTrue(sw.forcingArmed)
        sw.lap(3_000)
        assertEquals(350, sw.laps[1].totalCs)
    }
}
