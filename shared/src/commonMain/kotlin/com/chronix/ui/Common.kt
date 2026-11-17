package com.chronix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronix.domain.Lap
import com.chronix.domain.LapMark
import com.chronix.domain.Phase
import com.chronix.domain.lapMarks

/** Milisegundos que hay que mantener el botón de reinicio para armar el forzaje. */
const val ARM_HOLD_MS = 800L

/** Todo lo que necesita una pantalla de cronómetro, sea cual sea su diseño. */
class StopwatchScreenParams(
    val state: StopwatchState,
    val theme: ClockTheme,
    val onArmForcing: () -> Unit,
) {
    val sw get() = state.sw

    // Leer `rev` suscribe a quien lo lee a los cambios de fase y vueltas (el Stopwatch no es estado de Compose).
    val phase: Phase get() { state.rev; return state.sw.phase }
    val laps: List<Lap> get() { state.rev; return state.sw.laps }
    val now: Long get() = state.now
    val marks: Map<Int, LapMark> get() = if (theme.colorLaps) lapMarks(state.sw.laps) else emptyMap()

    fun lapColor(lap: Lap, normal: androidx.compose.ui.graphics.Color) = when (marks[lap.number]) {
        LapMark.BEST -> theme.lapBest
        LapMark.WORST -> theme.lapWorst
        else -> normal
    }
}

/** Refresca el reloj por fotograma mientras el cronómetro corre. */
@Composable
fun TickEffect(state: StopwatchState) {
    LaunchedEffect(state.rev) {
        while (state.sw.running) withFrameMillis { state.tick() }
    }
}

/**
 * Gesto del botón de reinicio: pulsación corta = restablecer; larga = restablecer + armar el forzaje (vibra).
 * Igual en todos los diseños.
 */
@Composable
fun Modifier.resetGesture(p: StopwatchScreenParams): Modifier =
    blocksSecret().pressAndHold(ARM_HOLD_MS, onHold = p.onArmForcing, onTap = { p.state.reset() })

/** Barra inferior plana: icono + etiqueta. Las pestañas no hacen nada en la fase 1. */
@Composable
fun FlatBottomBar(theme: ClockTheme, labelSize: Int = 11, boldSelected: Boolean = false, filledSelected: Boolean = true) {
    Row(
        Modifier.fillMaxWidth().background(theme.navBg).blocksSecret().padding(top = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        theme.tabs.forEachIndexed { i, tab ->
            val sel = i == theme.selectedTab
            val color = if (sel) theme.navSelected else theme.navIcon
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                IconView(tab.icon, color, 26.dp, filled = sel && filledSelected)
                Text(
                    tab.label, color = color, fontSize = labelSize.sp, maxLines = 1,
                    fontWeight = if (sel && boldSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

/** Barra inferior con píldora tras el icono seleccionado y etiquetas (estilo Material 3 / Pixel). */
@Composable
fun PillBottomBar(theme: ClockTheme) {
    Row(
        Modifier.fillMaxWidth().background(theme.navBg).blocksSecret().padding(top = 12.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        theme.tabs.forEachIndexed { i, tab ->
            val sel = i == theme.selectedTab
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.width(64.dp).height(32.dp).clip(RoundedCornerShape(50))
                        .background(if (sel) theme.navSelectedBg else androidx.compose.ui.graphics.Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) { IconView(tab.icon, if (sel) theme.navSelected else theme.navIcon, 22.dp, filled = sel) }
                Text(
                    tab.label, color = if (sel) theme.navSelected else theme.navIcon, fontSize = 11.sp, maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/**
 * Estado de la lista de vueltas (la última arriba): al registrar una vuelta salta a ella, para que siempre se vea
 * la última aunque las vueltas ya no quepan en pantalla (si no, Compose mantiene fija la primera fila visible).
 */
@Composable
fun rememberLapListState(lapCount: Int): androidx.compose.foundation.lazy.LazyListState {
    val state = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(lapCount) { if (lapCount > 0) state.animateScrollToItem(0) }
    return state
}
