package com.chronix.ui

import com.chronix.domain.ForceMode
import com.chronix.domain.ForcePlan
import com.chronix.domain.ForceTarget
import com.chronix.domain.StopSubmode

/** Almacenamiento clave-valor local. Android: SharedPreferences. iOS: NSUserDefaults (ver las `actual`). */
interface KeyValueStore {
    fun getString(key: String): String?
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getLong(key: String, default: Long): Long
    fun putString(key: String, value: String)
    fun putBoolean(key: String, value: Boolean)
    fun putLong(key: String, value: Long)
    fun remove(key: String)
}

/** Máximo del contador completo forzado al parar: 99 min 59 s 99 cs, en centésimas. Lo comparten los ajustes locales y los remotos. */
const val STOP_FULL_CS_MAX: Long = 99L * 6000 + 59 * 100 + 99

/** Ajustes secretos de la app. */
data class AppSettings(
    /** Look and feel elegido (id de [ClockThemes]). */
    val themeId: String = "samsung",
    /** Milisegundos de pulsación larga en la esquina superior derecha para abrir los ajustes. */
    val secretHoldMs: Long = 1500L,
    /** Centésimas forzadas por vuelta (posición 0 = vuelta 1). Lista ilimitada, 4 por defecto. */
    val forcedCs: List<Int> = listOf(0, 0, 0, 0),
    /** Dónde se fuerzan las centésimas: en el tiempo de vuelta (por defecto) o en el tiempo total. */
    val forceTarget: ForceTarget = ForceTarget.LAP,
    /** Mostrar el tiempo total en la tabla de vueltas (los diseños que lo traen). Si se oculta, no se puede forzar el total. */
    val showTotals: Boolean = true,
    /** Cuándo actúa el forzaje: en cada vuelta o al pulsar Parar (modo cronómetro). */
    val forceMode: ForceMode = ForceMode.LAPS,
    /** Modo cronómetro: forzar solo las centésimas del contador grande o el contador completo. */
    val stopSubmode: StopSubmode = StopSubmode.CENTIS,
    /** Modo cronómetro / centésimas: centésimas (0..99) con las que se detiene el contador grande al parar. */
    val stopCentis: Int = 0,
    /** Modo cronómetro / contador completo: tiempo (en centésimas de segundo) con el que se detiene el contador al parar. */
    val stopFullCs: Long = 0L,
    /** Icono de la app (id de [AppIconCatalog.icons]). Solo cambia al confirmarlo en el modal. */
    val iconId: String = AppIconCatalog.DEFAULT_ICON,
    /** Nombre de la app (id de [AppIconCatalog.names]). Solo Android. */
    val nameId: String = AppIconCatalog.DEFAULT_NAME,
    /** Mostrar la guía de inicio al abrir la app. Activa al instalar; se puede desactivar al final de la guía o en ajustes. */
    val showGuide: Boolean = true,
    /** Usar Magic Friends en este móvil: si está activo, un ayudante puede cambiar en remoto los ajustes de forzaje. Solo con esto se abre la red. */
    val magicFriendsEnabled: Boolean = false,
)

/** ¿Se ven los totales? Depende del ajuste y de que el diseño los tenga (el iPhone no). */
fun AppSettings.totalsVisible(): Boolean = showTotals && ClockThemes.byId(themeId).showsTotal

/**
 * Plan de forzaje de estos ajustes. Con los totales ocultos, «total» no existe y se fuerza la vuelta.
 * Si hay ajustes remotos ([remote], Magic Friends activo) se solapan sobre los locales: `remoto[clave] ?: local[clave]`.
 * Es el único sitio donde se construye el [ForcePlan].
 */
fun AppSettings.forcePlan(remote: RemoteOverrides? = null): ForcePlan {
    val s = if (remote == null) this else withRemote(remote)
    return ForcePlan(
        mode = s.forceMode,
        lapCs = s.forcedCs,
        lapTarget = if (s.totalsVisible()) s.forceTarget else ForceTarget.LAP,
        stopSubmode = s.stopSubmode,
        stopCentis = s.stopCentis,
        stopFullCs = s.stopFullCs,
    )
}

class SettingsStore(val prefs: KeyValueStore) {
    fun load(): AppSettings {
        val d = AppSettings()
        val forced = prefs.getString("forcedCs")?.let { raw ->
            if (raw.isBlank()) emptyList() else raw.split(",").mapNotNull { it.trim().toIntOrNull()?.coerceIn(0, 99) }
        } ?: d.forcedCs
        return AppSettings(
            themeId = prefs.getString("themeId") ?: d.themeId,
            secretHoldMs = prefs.getLong("secretHoldMs", d.secretHoldMs).coerceIn(300L, 10_000L),
            forcedCs = forced,
            forceTarget = if (prefs.getString("forceTarget") == "TOTAL") ForceTarget.TOTAL else ForceTarget.LAP,
            iconId = prefs.getString("iconId")?.takeIf { id -> AppIconCatalog.icons.any { it.id == id } } ?: d.iconId,
            nameId = prefs.getString("nameId")?.takeIf { id -> AppIconCatalog.names.any { it.id == id } } ?: d.nameId,
            showGuide = prefs.getBoolean("showGuide", d.showGuide),
            showTotals = prefs.getBoolean("showTotals", d.showTotals),
            forceMode = if (prefs.getString("forceMode") == "STOPWATCH") ForceMode.STOPWATCH else ForceMode.LAPS,
            stopSubmode = if (prefs.getString("stopSubmode") == "FULL") StopSubmode.FULL else StopSubmode.CENTIS,
            stopCentis = prefs.getLong("stopCs", d.stopCentis.toLong()).toInt().coerceIn(0, 99),
            stopFullCs = prefs.getLong("stopFullCs", d.stopFullCs).coerceIn(0L, STOP_FULL_CS_MAX),
            magicFriendsEnabled = prefs.getBoolean("mfEnabled", d.magicFriendsEnabled),
        )
    }

    fun save(s: AppSettings) {
        prefs.putString("themeId", s.themeId)
        prefs.putLong("secretHoldMs", s.secretHoldMs)
        prefs.putString("forcedCs", s.forcedCs.joinToString(","))
        prefs.putString("forceTarget", s.forceTarget.name)
        prefs.putString("iconId", s.iconId)
        prefs.putString("nameId", s.nameId)
        prefs.putBoolean("showGuide", s.showGuide)
        prefs.putBoolean("showTotals", s.showTotals)
        prefs.putString("forceMode", s.forceMode.name)
        prefs.putString("stopSubmode", s.stopSubmode.name)
        prefs.putLong("stopCs", s.stopCentis.toLong())
        prefs.putLong("stopFullCs", s.stopFullCs)
        prefs.putBoolean("mfEnabled", s.magicFriendsEnabled)
    }
}
