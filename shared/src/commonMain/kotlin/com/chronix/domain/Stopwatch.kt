package com.chronix.domain

/** Una vuelta registrada. Los tiempos van en centésimas de segundo. */
data class Lap(val number: Int, val lapCs: Long, val totalCs: Long)

enum class Phase { IDLE, RUNNING, PAUSED }

enum class LapMark { NONE, BEST, WORST }

/** Qué cifra de la vuelta lleva las centésimas forzadas: la del tiempo total o la del tiempo de vuelta. */
enum class ForceTarget { TOTAL, LAP }

/** Cuándo actúa el forzaje: en cada vuelta ([LAPS]) o una vez al pulsar Parar ([STOPWATCH], «modo cronómetro»). */
enum class ForceMode { LAPS, STOPWATCH }

/** Qué se fuerza al parar: solo las centésimas del contador grande, o el contador completo (minutos, segundos y centésimas). */
enum class StopSubmode { CENTIS, FULL }

/**
 * Qué forzar al armar el forzaje. Con [ForceMode.LAPS] se usan [lapCs] y [lapTarget] (una posición por vuelta). Con
 * [ForceMode.STOPWATCH] hay **un solo valor** que se aplica en el primer Parar tras armar: [stopCentis] (centésimas
 * 0..99, si [stopSubmode] es CENTIS) o [stopFullCs] (tiempo completo en centésimas de segundo, si es FULL).
 */
class ForcePlan(
    val mode: ForceMode = ForceMode.LAPS,
    val lapCs: List<Int> = emptyList(),
    val lapTarget: ForceTarget = ForceTarget.LAP,
    val stopSubmode: StopSubmode = StopSubmode.CENTIS,
    val stopCentis: Int = 0,
    val stopFullCs: Long = 0L,
)

/**
 * Cronómetro puro (sin UI ni reloj propio): todas las operaciones reciben `nowMs`, un instante monotónico en
 * milisegundos. Así es determinista y testeable, y no deriva (se calcula por diferencia, no por ticks).
 *
 * Incluye el **modo forzaje** ([ForcePlan]). Por vueltas: la n-ésima vuelta fija las centésimas de una de sus cifras
 * ([ForceTarget.TOTAL] o [ForceTarget.LAP]) al valor configurado para n; la otra cifra se obtiene por diferencia o por
 * suma, así la tabla siempre cuadra. Modo cronómetro: en el primer Parar tras armar se fuerza el contador grande (solo
 * centésimas o contador completo) y la vuelta en curso se ajusta sola (= contador − total de la última vuelta).
 *
 * **Regla general:** lo que se fuerza se sustituye siempre, tal cual (el contador completo puede incluso retroceder). Lo
 * que NO se fuerza nunca viaja atrás: cuando solo se fuerzan centésimas, los minutos y segundos se quedan en los que
 * estamos o pasan al siguiente.
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
    private var target = ForceTarget.TOTAL
    private var mode = ForceMode.LAPS
    private var stopSubmode = StopSubmode.CENTIS
    private var stopValue = 0L
    private var stopPending = false // el forzaje del Parar sigue sin consumir

    val running: Boolean get() = phase == Phase.RUNNING

    /** True mientras queden posiciones por forzar (vueltas o paradas). Solo para lógica/tests: la UI nunca debe mostrarlo. */
    val forcingArmed: Boolean
        get() = if (mode == ForceMode.STOPWATCH) stopPending else forcedIndex < forced.size

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
        if (mode == ForceMode.STOPWATCH && stopPending) {
            stopPending = false
            val real = baseMs / 10
            val last = laps.lastOrNull()?.totalCs ?: 0L
            baseMs = if (stopSubmode == StopSubmode.FULL) {
                // Contador completo: se fuerzan minutos, segundos y centésimas, así que se sustituye tal cual (aunque retroceda)
                stopValue * 10
            } else {
                // Solo centésimas: los minutos y segundos no se fuerzan y nunca viajan atrás (al alza)
                forceTotal(real, last, stopValue.toInt()) * 10
            }
        }
        phase = Phase.PAUSED
    }

    fun lap(nowMs: Long) {
        if (!running) return
        val prev = laps.lastOrNull()?.totalCs ?: 0L
        val real = elapsedCs(nowMs)
        val lapCs: Long
        val total: Long
        if (mode == ForceMode.LAPS && forcedIndex < forced.size) {
            val f = forced[forcedIndex++]
            if (target == ForceTarget.LAP) {
                lapCs = forceLap(real - prev, f)
                total = prev + lapCs
            } else {
                total = forceTotal(real, prev, f)
                lapCs = total - prev
            }
        } else {
            total = maxOf(real, prev + 1)
            lapCs = total - prev
        }
        laps = laps + Lap(laps.size + 1, lapCs, total)
    }

    /** Restablecer normal: a cero y sin forzaje. */
    fun reset() {
        phase = Phase.IDLE
        baseMs = 0L
        laps = emptyList()
        forced = emptyList()
        forcedIndex = 0
        mode = ForceMode.LAPS
        stopValue = 0L
        stopPending = false
    }

    /** Restablecer + armar el forzaje por vueltas con la lista de centésimas (cada valor se limita a 0..99). */
    fun resetAndArm(forcedCs: List<Int>, target: ForceTarget = ForceTarget.TOTAL) =
        resetAndArm(ForcePlan(ForceMode.LAPS, lapCs = forcedCs, lapTarget = target))

    /** Restablecer + armar el forzaje del [plan]. */
    fun resetAndArm(plan: ForcePlan) {
        reset()
        mode = plan.mode
        forced = plan.lapCs.map { it.coerceIn(0, 99) }
        target = plan.lapTarget
        stopSubmode = plan.stopSubmode
        stopValue = if (plan.stopSubmode == StopSubmode.CENTIS) plan.stopCentis.coerceIn(0, 99).toLong()
        else plan.stopFullCs.coerceAtLeast(0L)
        stopPending = plan.mode == ForceMode.STOPWATCH
    }

    companion object {
        /**
         * Total forzado: el menor valor **>= `realCs`** cuyas centésimas sean `f` (mismo segundo si las centésimas
         * reales aún no han pasado de `f`; si ya las pasaron, el segundo siguiente). Nunca resta. Si no supera el
         * total anterior (`prevTotal`), sube de 1 en 1 segundo hasta superarlo, para que la vuelta sea siempre > 0.
         */
        fun forceTotal(realCs: Long, prevTotal: Long, f: Int): Long {
            var t = (realCs / 100) * 100 + f
            if (t < realCs) t += 100
            while (t <= prevTotal || t < 0) t += 100
            return t
        }

        /**
         * Tiempo de vuelta forzado: el menor valor **>= `realLapCs`** con centésimas `f` y siempre positivo
         * (si la vuelta real ya es <= 0 porque la anterior se adelantó, sale la primera duración positiva con `f`).
         */
        fun forceLap(realLapCs: Long, f: Int): Long {
            var l = (realLapCs / 100) * 100 + f
            if (l < realLapCs) l += 100
            while (l <= 0) l += 100
            return l
        }
    }
}

/** `MM:SS,cc`, o `H:MM:SS,cc` a partir de la hora. `decimal` es el separador de las centésimas. */
fun formatCs(cs: Long, decimal: String = ","): String {
    val c = (cs % 100).toInt()
    val totalSec = cs / 100
    val s = (totalSec % 60).toInt()
    val m = ((totalSec / 60) % 60).toInt()
    val h = totalSec / 3600
    val core = "${two(m)}:${two(s)}$decimal${two(c)}"
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
