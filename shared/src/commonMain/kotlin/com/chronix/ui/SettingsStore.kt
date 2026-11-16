package com.chronix.ui

/** Almacenamiento clave-valor local. Android: SharedPreferences. iOS: NSUserDefaults (ver las `actual`). */
interface KeyValueStore {
    fun getString(key: String): String?
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getLong(key: String, default: Long): Long
    fun putString(key: String, value: String)
    fun putBoolean(key: String, value: Boolean)
    fun putLong(key: String, value: Long)
}

/** Ajustes secretos de la app. */
data class AppSettings(
    /** Look and feel elegido (id de [ClockThemes]). */
    val themeId: String = "samsung",
    /** Milisegundos de pulsación larga en la esquina superior derecha para abrir los ajustes. */
    val secretHoldMs: Long = 1500L,
    /** Centésimas forzadas por vuelta (posición 0 = vuelta 1). Lista ilimitada, 4 por defecto. */
    val forcedCs: List<Int> = listOf(0, 0, 0, 0),
)

class SettingsStore(private val prefs: KeyValueStore) {
    fun load(): AppSettings {
        val d = AppSettings()
        val forced = prefs.getString("forcedCs")?.let { raw ->
            if (raw.isBlank()) emptyList() else raw.split(",").mapNotNull { it.trim().toIntOrNull()?.coerceIn(0, 99) }
        } ?: d.forcedCs
        return AppSettings(
            themeId = prefs.getString("themeId") ?: d.themeId,
            secretHoldMs = prefs.getLong("secretHoldMs", d.secretHoldMs).coerceIn(300L, 10_000L),
            forcedCs = forced,
        )
    }

    fun save(s: AppSettings) {
        prefs.putString("themeId", s.themeId)
        prefs.putLong("secretHoldMs", s.secretHoldMs)
        prefs.putString("forcedCs", s.forcedCs.joinToString(","))
    }
}
