package com.chronix.ui

import androidx.compose.ui.graphics.Color

/**
 * Look and feel del reloj. Solo datos (colores y textos): la lógica del cronómetro no sabe nada de esto.
 * Para añadir un fabricante nuevo basta con crear otro `ClockTheme` y añadirlo a [ClockThemes.all].
 */
data class ClockTheme(
    val id: String,
    val name: String,
    val bgTop: Color,
    val bgBottom: Color,
    val text: Color,
    val textSecondary: Color,
    val buttonSecondaryBg: Color,
    val buttonPrimaryBg: Color,
    val buttonPrimaryText: Color,
    val buttonStopBg: Color,
    val buttonStopText: Color,
    val lapBest: Color,
    val lapWorst: Color,
    val navBg: Color,
    val navSelectedBg: Color,
    val navIcon: Color,
    // Textos
    val lapLabel: String = "Vuelta",
    val startLabel: String = "Iniciar",
    val stopLabel: String = "Parar",
    val resumeLabel: String = "Reanudar",
    val resetLabel: String = "Restablecer",
    val headerLap: String = "Vuelta",
    val headerLapTime: String = "Tiempos de vuelta",
    val headerTotal: String = "Tiempo total",
)

object ClockThemes {
    /** Samsung One UI (capturas en data/Visuales/Samsung *.jpeg). */
    val Samsung = ClockTheme(
        id = "samsung",
        name = "Samsung (One UI)",
        bgTop = Color(0xFFFCFCFE),
        bgBottom = Color(0xFFE9ECFC),
        text = Color(0xFF000000),
        textSecondary = Color(0xFF7A7A80),
        buttonSecondaryBg = Color(0xFFFAFAFA),
        buttonPrimaryBg = Color(0xFFC6C8F4),
        buttonPrimaryText = Color(0xFF3F3FE0),
        buttonStopBg = Color(0xFFF8D7D7),
        buttonStopText = Color(0xFFD33A35),
        lapBest = Color(0xFF5B5BD6),
        lapWorst = Color(0xFFD33A35),
        navBg = Color(0xFFF6F6F6),
        navSelectedBg = Color(0xFFDDDDDF),
        navIcon = Color(0xFF111111),
    )

    val all: List<ClockTheme> = listOf(Samsung)

    fun byId(id: String): ClockTheme = all.firstOrNull { it.id == id } ?: Samsung
}
