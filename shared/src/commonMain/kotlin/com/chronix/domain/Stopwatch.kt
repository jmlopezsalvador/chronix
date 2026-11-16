package com.chronix.domain

/** Una vuelta registrada. Los tiempos van en centésimas de segundo. */
data class Lap(val number: Int, val lapCs: Long, val totalCs: Long)

enum class Phase { IDLE, RUNNING, PAUSED }

enum class LapMark { NONE, BEST, WORST }

/**
 * Cronómetro puro (sin UI ni reloj propio): todas las operaciones reciben `nowMs`, un instante monotónico en
 * milisegundos. Así es determinista y testeable, y no deriva (se calcula por diferencia, no por ticks).
 *
 * Incluye el **modo forzaje**: con él armado, la n-ésima vuelta fija las centésimas de su tiempo total
 * al valor configurado para n. El tiempo de vuelta se obtiene por diferencia, así la tabla siempre cuadra.
 */
class Stopwatch {
    var phase: Phase = Phase.IDLE
        private set
    var laps: List<Lap> = emptyList()
        private set

    private var baseMs = 0L
    private var startedAt = 0L
    private var forced: List<Int> = emptyList()
    private var forcedIndex = 0

    val running: Boolean get() = phase == Phase.RUNNING

    /** True mientras queden vueltas por forzar. Solo para lógica/tests: la UI nunca debe mostrarlo. */
    val forcingArmed: Boolean get() = forcedIndex < forced.size

    fun elapsedMs(nowMs: Long): Long = if (running) baseMs + (nowMs - startedAt) else baseMs

    fun elapsedCs(nowMs: Long): Long = elapsedMs(nowMs) / 10

    /** Tiempo de la vuelta en curso (desde la última vuelta registrada). */
    fun currentLapCs(nowMs: Long): Long = (elapsedCs(nowMs) - (laps.lastOrNull()?.totalCs ?: 0L)).coerceAtLeast(0L)

    fun start(nowMs: Long) {
        if (running) return
        startedAt = nowMs
        phase = Phase.RUNNING
    }

    fun stop(nowMs: Long) {
        if (!running) return
        baseMs = elapsedMs(nowMs)
        phase = Phase.PAUSED
    }

    fun lap(nowMs: Long) {
        if (!running) return
        val prev = laps.lastOrNull()?.totalCs ?: 0L
        val real = elapsedCs(nowMs)
        val total = if (forcingArmed) {
            forceTotal(real, prev, forced[forcedIndex]).also { forcedIndex++ }
        } else {
            maxOf(real, prev + 1)
        }
        laps = laps + Lap(laps.size + 1, total - prev, total)
    }

    /** Restablecer normal: a cero y sin forzaje. */
    fun reset() {
        phase = Phase.IDLE
        baseMs = 0L
        laps = emptyList()
        forced = emptyList()
        forcedIndex = 0
    }

    /** Restablecer + armar el forzaje con la lista de centésimas (cada valor se limita a 0..99). */
    fun resetAndArm(forcedCs: List<Int>) {
        reset()
        forced = forcedCs.map { it.coerceIn(0, 99) }
    }

    companion object {
        /**
         * Total forzado: el mayor valor <= `realCs` cuyas centésimas sean `f`. Si no supera el total anterior
         * (`prevTotal`), sube de 1 en 1 segundo hasta superarlo, para que la vuelta nunca sea cero ni negativa.
         */
        fun forceTotal(realCs: Long, prevTotal: Long, f: Int): Long {
            var t = (realCs / 100) * 100 + f
            if (t > realCs) t -= 100
            while (t <= prevTotal || t < 0) t += 100
            return t
        }
    }
}

/** `MM:SS,cc`, o `H:MM:SS,cc` a partir de la hora. */
fun formatCs(cs: Long): String {
    val c = (cs % 100).toInt()
    val totalSec = cs / 100
    val s = (totalSec % 60).toInt()
    val m = ((totalSec / 60) % 60).toInt()
    val h = totalSec / 3600
    val core = "${two(m)}:${two(s)},${two(c)}"
    return if (h > 0) "$h:$core" else core
}

private fun two(n: Int) = if (n < 10) "0$n" else "$n"

/**
 * Colores de vuelta: la primera vuelta nunca se colorea. Entre la 2.ª y la última, la más corta es BEST y la más
 * larga WORST (necesita al menos 2 vueltas coloreables y que no todas duren lo mismo).
 */
fun lapMarks(laps: List<Lap>): Map<Int, LapMark> {
    val candidates = laps.drop(1)
    if (candidates.size < 2) return emptyMap()
    val best = candidates.minBy { it.lapCs }
    val worst = candidates.maxBy { it.lapCs }
    if (best.lapCs == worst.lapCs) return emptyMap()
    return mapOf(best.number to LapMark.BEST, worst.number to LapMark.WORST)
}
