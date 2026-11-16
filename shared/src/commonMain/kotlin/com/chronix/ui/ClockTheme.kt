package com.chronix.ui

import androidx.compose.ui.graphics.Color

/** Diseño (maquetación) del cronómetro. Cada uno tiene su composable en `ui/styles/`. */
enum class ClockStyle { SAMSUNG, IPHONE, PIXEL, ONEPLUS, OPPO, XIAOMI, HUAWEI }

/** Pestaña de la barra inferior (solo la del cronómetro funciona en la fase 1). */
data class ClockTab(val label: String, val icon: IconKind)

/**
 * Look and feel del reloj de un fabricante. Solo datos (colores, textos, pestañas): la lógica del cronómetro
 * no sabe nada de esto. Para añadir un fabricante: crear un `ClockTheme` y añadirlo a [ClockThemes.all],
 * y su composable en `ui/styles/`.
 */
data class ClockTheme(
    val id: String,
    val name: String,
    val style: ClockStyle,
    /** Fondo oscuro: sirve para poner los iconos de la barra de estado en claro. */
    val dark: Boolean,
    val bgTop: Color,
    val bgBottom: Color,
    val surface: Color,
    val text: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentOn: Color,
    val stopBg: Color,
    val stopText: Color,
    val buttonBg: Color,
    val buttonText: Color,
    val lapBest: Color,
    val lapWorst: Color,
    /** Si el diseño colorea la vuelta más corta/larga. */
    val colorLaps: Boolean,
    val navBg: Color,
    val navSelectedBg: Color,
    val navIcon: Color,
    val navSelected: Color,
    val tabs: List<ClockTab>,
    val selectedTab: Int,
    val decimal: String = ",",
    val title: String = "Cronómetro",
    val lapLabel: String = "Vuelta",
    val startLabel: String = "Iniciar",
    val stopLabel: String = "Parar",
    val resumeLabel: String = "Reanudar",
    val resetLabel: String = "Restablecer",
    val headerLap: String = "Vuelta",
    val headerLapTime: String = "Tiempo de vuelta",
    val headerTotal: String = "Tiempo total",
)

object ClockThemes {
    private val white = Color.White

    /** Samsung One UI (capturas en data/Visuales/Samsung *.jpeg). */
    val Samsung = ClockTheme(
        id = "samsung", name = "Samsung (One UI)", style = ClockStyle.SAMSUNG, dark = false,
        bgTop = Color(0xFFFCFCFE), bgBottom = Color(0xFFE9ECFC), surface = Color(0xFFFAFAFA),
        text = Color(0xFF000000), textSecondary = Color(0xFF7A7A80),
        accent = Color(0xFFC6C8F4), accentOn = Color(0xFF3F3FE0),
        stopBg = Color(0xFFF8D7D7), stopText = Color(0xFFD33A35),
        buttonBg = Color(0xFFFAFAFA), buttonText = Color(0xFF000000),
        lapBest = Color(0xFF5B5BD6), lapWorst = Color(0xFFD33A35), colorLaps = true,
        navBg = Color(0xFFF6F6F6), navSelectedBg = Color(0xFFDDDDDF), navIcon = Color(0xFF111111), navSelected = Color(0xFF111111),
        tabs = listOf(
            ClockTab("Alarma", IconKind.ALARM), ClockTab("Reloj mundial", IconKind.WORLD),
            ClockTab("Cronómetro", IconKind.STOPWATCH), ClockTab("Temporizador", IconKind.HOURGLASS),
        ),
        selectedTab = 2,
        headerLapTime = "Tiempos de vuelta",
    )

    /** iPhone (iOS 17): fondo negro, acento naranja, vista digital y analógica deslizables. */
    val IPhone = ClockTheme(
        id = "iphone", name = "iPhone (iOS)", style = ClockStyle.IPHONE, dark = true,
        bgTop = Color(0xFF000000), bgBottom = Color(0xFF000000), surface = Color(0xFF1C1C1E),
        text = Color(0xFFFFFFFF), textSecondary = Color(0xFF8E8E93),
        accent = Color(0xFF0C3B1D), accentOn = Color(0xFF30D158),
        stopBg = Color(0xFF3A0F0D), stopText = Color(0xFFFF453A),
        buttonBg = Color(0xFF333335), buttonText = Color(0xFFFFFFFF),
        lapBest = Color(0xFF30D158), lapWorst = Color(0xFFFF453A), colorLaps = true,
        navBg = Color(0xFF0B0B0B), navSelectedBg = Color.Transparent, navIcon = Color(0xFF8E8E93), navSelected = Color(0xFFFF9F0A),
        tabs = listOf(
            ClockTab("Reloj mundial", IconKind.WORLD), ClockTab("Alarma", IconKind.ALARM),
            ClockTab("Cronómetro", IconKind.STOPWATCH), ClockTab("Temporizador", IconKind.CLOCK),
        ),
        selectedTab = 2,
        startLabel = "Iniciar", stopLabel = "Parar", resetLabel = "Reiniciar", resumeLabel = "Iniciar",
    )

    /** Google Pixel (Clock 8.1, Material 3 Expressive, tema oscuro). */
    val Pixel = ClockTheme(
        id = "pixel", name = "Google Pixel", style = ClockStyle.PIXEL, dark = true,
        bgTop = Color(0xFF00121A), bgBottom = Color(0xFF00121A), surface = Color(0xFF03242F),
        text = Color(0xFFC2F0FF), textSecondary = Color(0xFF7FA8B8),
        accent = Color(0xFF96F0FF), accentOn = Color(0xFF00363F),
        stopBg = Color(0xFFFF7A70), stopText = Color(0xFF5A0A06),
        buttonBg = Color(0xFF032A36), buttonText = Color(0xFFD6F3FF),
        lapBest = Color(0xFF7FB2FF), lapWorst = Color(0xFFFF8A80), colorLaps = true,
        navBg = Color(0xFF0E2630), navSelectedBg = Color(0xFF00677A), navIcon = Color(0xFF7FA8B8), navSelected = Color(0xFF8FF0FF),
        tabs = listOf(
            ClockTab("Alarma", IconKind.ALARM), ClockTab("Reloj", IconKind.CLOCK), ClockTab("Temporizador", IconKind.HOURGLASS),
            ClockTab("Cronómetro", IconKind.STOPWATCH), ClockTab("Hora de dormir", IconKind.BED),
        ),
        selectedTab = 3,
        startLabel = "Iniciar", stopLabel = "Detener", resumeLabel = "Reanudar", resetLabel = "Restablecer",
    )

    /** OnePlus (OxygenOS 12): aro azul que se dibuja alrededor del tiempo. */
    val OnePlus = ClockTheme(
        id = "oneplus", name = "OnePlus (OxygenOS)", style = ClockStyle.ONEPLUS, dark = false,
        bgTop = Color(0xFFFAFAFA), bgBottom = Color(0xFFFAFAFA), surface = Color(0xFFFFFFFF),
        text = Color(0xFF202124), textSecondary = Color(0xFF8A8A8E),
        accent = Color(0xFF1F43C0), accentOn = Color(0xFFFFFFFF),
        stopBg = Color(0xFF1F43C0), stopText = Color(0xFFFFFFFF),
        buttonBg = Color(0xFFFFFFFF), buttonText = Color(0xFF202124),
        lapBest = Color(0xFF26C281), lapWorst = Color(0xFFDC2F5A), colorLaps = true,
        navBg = Color(0xFFFFFFFF), navSelectedBg = Color.Transparent, navIcon = Color(0xFF9E9E9E), navSelected = Color(0xFF111111),
        tabs = listOf(
            ClockTab("Alarma", IconKind.ALARM), ClockTab("Reloj mundial", IconKind.CLOCK),
            ClockTab("Temporizador", IconKind.HOURGLASS), ClockTab("Cronómetro", IconKind.STOPWATCH),
        ),
        selectedTab = 3,
        headerLapTime = "Tiempo de vuelta", headerTotal = "Tiempo parcial",
    )

    /** OPPO (ColorOS 13): esfera blanca con relieve y resplandor azul violeta. */
    val Oppo = ClockTheme(
        id = "oppo", name = "OPPO (ColorOS)", style = ClockStyle.OPPO, dark = false,
        bgTop = Color(0xFFF5F5F7), bgBottom = Color(0xFFF5F5F7), surface = Color(0xFFF7F7F9),
        text = Color(0xFF202020), textSecondary = Color(0xFF6E6E73),
        accent = Color(0xFF2B3BE8), accentOn = Color(0xFFFFFFFF),
        stopBg = Color(0xFF2B3BE8), stopText = Color(0xFFFFFFFF),
        buttonBg = Color(0xFFF7F7F9), buttonText = Color(0xFF202020),
        lapBest = Color(0xFFE02B2B), lapWorst = Color(0xFF2B3BE8), colorLaps = true,
        navBg = Color(0xFFFFFFFF), navSelectedBg = Color.Transparent, navIcon = Color(0xFF8A8A8E), navSelected = Color(0xFF111111),
        tabs = listOf(
            ClockTab("Alarma", IconKind.ALARM), ClockTab("Reloj mundial", IconKind.WORLD),
            ClockTab("Cronómetro", IconKind.STOPWATCH), ClockTab("Temporizador", IconKind.HOURGLASS),
        ),
        selectedTab = 2,
        headerLap = "Vuelta", headerLapTime = "Tiempo de vuelta", headerTotal = "Total",
    )

    /** Xiaomi / Redmi / POCO (HyperOS 2): minimalista, acciones azules con iconos. */
    val Xiaomi = ClockTheme(
        id = "xiaomi", name = "Xiaomi (HyperOS)", style = ClockStyle.XIAOMI, dark = false,
        bgTop = Color(0xFFF7F7F7), bgBottom = Color(0xFFF7F7F7), surface = Color(0xFFFFFFFF),
        text = Color(0xFF000000), textSecondary = Color(0xFF7A7A7A),
        accent = Color(0xFF3482FF), accentOn = Color(0xFFFFFFFF),
        stopBg = Color(0xFFFFFFFF), stopText = Color(0xFF3482FF),
        buttonBg = Color(0xFFFFFFFF), buttonText = Color(0xFF3482FF),
        lapBest = Color(0xFF000000), lapWorst = Color(0xFF000000), colorLaps = false,
        navBg = Color(0xFFF7F7F7), navSelectedBg = Color.Transparent, navIcon = Color(0xFF8C8C8C), navSelected = Color(0xFF111111),
        tabs = listOf(
            ClockTab("Alarma", IconKind.ALARM), ClockTab("Reloj mundial", IconKind.CLOCK),
            ClockTab("Cronómetro", IconKind.STOPWATCH), ClockTab("Temporizador", IconKind.HOURGLASS),
        ),
        selectedTab = 2,
    )

    /** Huawei (EMUI 12): esfera analógica con subesfera de minutos y vueltas en tarjetas. */
    val Huawei = ClockTheme(
        id = "huawei", name = "Huawei (EMUI)", style = ClockStyle.HUAWEI, dark = false,
        bgTop = Color(0xFFF1F3F5), bgBottom = Color(0xFFF1F3F5), surface = Color(0xFFFFFFFF),
        text = Color(0xFF000000), textSecondary = Color(0xFF8A8A8E),
        accent = Color(0xFF0A59F7), accentOn = Color(0xFFFFFFFF),
        stopBg = Color(0xFF0A59F7), stopText = Color(0xFFFFFFFF),
        buttonBg = Color(0xFFFFFFFF), buttonText = Color(0xFF000000),
        lapBest = Color(0xFF000000), lapWorst = Color(0xFF000000), colorLaps = false,
        navBg = Color(0xFFF1F3F5), navSelectedBg = Color.Transparent, navIcon = Color(0xFF8A8A8E), navSelected = Color(0xFF0A59F7),
        tabs = listOf(
            ClockTab("Alarma", IconKind.ALARM), ClockTab("Reloj", IconKind.WORLD),
            ClockTab("Cronómetro", IconKind.STOPWATCH), ClockTab("Temporizador", IconKind.HOURGLASS),
        ),
        selectedTab = 2,
    )

    val all: List<ClockTheme> = listOf(Samsung, IPhone, Pixel, OnePlus, Oppo, Xiaomi, Huawei)

    fun byId(id: String): ClockTheme = all.firstOrNull { it.id == id } ?: Samsung
}
