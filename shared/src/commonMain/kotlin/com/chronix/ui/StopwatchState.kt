package com.chronix.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import com.chronix.domain.ForcePlan
import com.chronix.domain.Stopwatch
import kotlin.time.TimeSource

private val processStart = TimeSource.Monotonic.markNow()

/** Milisegundos monotónicos desde el arranque del proceso (no les afectan los cambios de hora del sistema). */
fun monotonicNowMs(): Long = processStart.elapsedNow().inWholeMilliseconds

/** Envoltorio observable por Compose alrededor del [Stopwatch] puro. */
class StopwatchState {
    val sw = Stopwatch()

    /** Sube con cada acción para que la UI se recomponga. */
    var rev by mutableIntStateOf(0)
        private set

    /** Instante actual refrescado por fotograma mientras corre. */
    var now by mutableLongStateOf(monotonicNowMs())
        private set

    fun tick() { now = monotonicNowMs() }

    private fun touch() { now = monotonicNowMs(); rev++ }

    fun start() { sw.start(monotonicNowMs()); touch() }
    fun stop() { sw.stop(monotonicNowMs()); touch() }
    fun lap() { sw.lap(monotonicNowMs()); touch() }
    fun reset() { sw.reset(); touch() }
    fun resetAndArm(plan: ForcePlan) { sw.resetAndArm(plan); touch() }
}
