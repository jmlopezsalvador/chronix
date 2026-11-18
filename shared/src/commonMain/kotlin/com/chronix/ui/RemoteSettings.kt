package com.chronix.ui

import com.chronix.domain.ForceMode
import com.chronix.domain.ForceTarget
import com.chronix.domain.StopSubmode
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Capa de ajustes **remotos** de Magic Friends (lado mago). El ayudante solo puede cambiar los ajustes de predicción
 * (lista blanca [RemoteKeys]); el aspecto, el icono, el nombre, el acceso, la guía y `showTotals` son siempre locales.
 * La capa remota se guarda aparte (la librería la persiste) y nunca machaca los ajustes locales.
 *
 * Formato en el cable (JSON Merge Patch sobre un objeto; ver docs/SPEC.md §9):
 * - `forceMode`: `"LAPS"` | `"STOPWATCH"`
 * - `forceTarget`: `"LAP"` | `"TOTAL"`
 * - `stopSubmode`: `"CENTIS"` | `"FULL"`
 * - `forcedCs`: array de enteros (0..99, una posición por vuelta)
 * - `stopCentis`: entero (0..99)
 * - `stopFullCs`: entero (centésimas de segundo, 0..[STOP_FULL_CS_MAX])
 */
object RemoteKeys {
    const val FORCE_MODE = "forceMode"
    const val FORCE_TARGET = "forceTarget"
    const val STOP_SUBMODE = "stopSubmode"
    const val FORCED_CS = "forcedCs"
    const val STOP_CENTIS = "stopCentis"
    const val STOP_FULL_CS = "stopFullCs"

    val all: Set<String> = setOf(FORCE_MODE, FORCE_TARGET, STOP_SUBMODE, FORCED_CS, STOP_CENTIS, STOP_FULL_CS)
}

/** Valores remotos ya validados. `null` = esa clave no se solapa y manda el ajuste local. */
data class RemoteOverrides(
    val forceMode: ForceMode? = null,
    val forceTarget: ForceTarget? = null,
    val stopSubmode: StopSubmode? = null,
    val forcedCs: List<Int>? = null,
    val stopCentis: Int? = null,
    val stopFullCs: Long? = null,
)

/**
 * Valida el objeto remoto con los mismos límites que `SettingsStore.load()`:
 * - los números fuera de rango se **limitan** (`coerceIn`);
 * - lo que no tiene el tipo esperado, un enum desconocido o un `forcedCs` con algún elemento que no es número se **ignoran**
 *   (esa clave la decide el local). Un array con elementos descartados desplazaría las posiciones de las vueltas.
 * Las claves fuera de la lista blanca ([RemoteKeys]) ni se leen.
 */
fun parseRemoteOverrides(remote: JsonObject): RemoteOverrides {
    fun number(key: String, min: Long, max: Long): Long? {
        val p = remote[key] as? JsonPrimitive ?: return null
        if (p.isString) return null
        return p.longOrNull?.coerceIn(min, max)
    }

    fun <T> choice(key: String, vararg options: Pair<String, T>): T? {
        val p = remote[key] as? JsonPrimitive ?: return null
        if (!p.isString) return null
        return options.firstOrNull { it.first == p.content }?.second
    }

    val forcedCs = (remote[RemoteKeys.FORCED_CS] as? JsonArray)?.let { array ->
        val values = array.map { el -> (el as? JsonPrimitive)?.takeIf { !it.isString }?.longOrNull }
        if (values.any { it == null }) null else values.map { it!!.coerceIn(0L, 99L).toInt() }
    }

    return RemoteOverrides(
        forceMode = choice(RemoteKeys.FORCE_MODE, "LAPS" to ForceMode.LAPS, "STOPWATCH" to ForceMode.STOPWATCH),
        forceTarget = choice(RemoteKeys.FORCE_TARGET, "LAP" to ForceTarget.LAP, "TOTAL" to ForceTarget.TOTAL),
        stopSubmode = choice(RemoteKeys.STOP_SUBMODE, "CENTIS" to StopSubmode.CENTIS, "FULL" to StopSubmode.FULL),
        forcedCs = forcedCs,
        stopCentis = number(RemoteKeys.STOP_CENTIS, 0L, 99L)?.toInt(),
        stopFullCs = number(RemoteKeys.STOP_FULL_CS, 0L, STOP_FULL_CS_MAX),
    )
}

/** Ajustes efectivos: `remoto[clave] ?: local[clave]` solo para las claves de la lista blanca. */
fun AppSettings.withRemote(o: RemoteOverrides): AppSettings = copy(
    forceMode = o.forceMode ?: forceMode,
    forceTarget = o.forceTarget ?: forceTarget,
    stopSubmode = o.stopSubmode ?: stopSubmode,
    forcedCs = o.forcedCs ?: forcedCs,
    stopCentis = o.stopCentis ?: stopCentis,
    stopFullCs = o.stopFullCs ?: stopFullCs,
)
