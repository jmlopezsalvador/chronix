package com.chronix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.chronix.domain.LapMark
import com.chronix.domain.Phase
import com.chronix.domain.formatCs
import com.chronix.domain.lapMarks

/** Milisegundos que hay que mantener *Restablecer* para armar el forzaje. */
private const val ARM_HOLD_MS = 800L

/**
 * Pantalla del cronómetro con el aspecto de [theme]. Es una réplica fiel del reloj real:
 * no muestra nada del forzaje. [onSecret] se dispara con la pulsación larga en la esquina superior derecha.
 */
@Composable
fun StopwatchScreen(
    state: StopwatchState,
    theme: ClockTheme,
    secretHoldMs: Long,
    onSecret: () -> Unit,
    onArmForcing: () -> Unit,
) {
    val sw = state.sw
    // Refresco por fotograma mientras corre (rev cambia con cada acción y relanza el efecto).
    LaunchedEffect(state.rev) {
        while (sw.running) withFrameMillis { state.tick() }
    }
    val now = state.now
    val phase = sw.phase
    val laps = sw.laps
    val marks = lapMarks(laps)
    val timeStyle = TextStyle(
        fontSize = 76.sp, fontWeight = FontWeight.SemiBold, color = theme.text,
        textAlign = TextAlign.Center, fontFeatureSettings = "tnum",
    )

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(theme.bgTop, theme.bgBottom)))) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            // Barra superior: menú de tres puntos (decorativo)
            Row(Modifier.fillMaxWidth().height(72.dp).padding(end = 24.dp), horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(24.dp)) {
                    val r = size.width * 0.075f
                    for (i in 0..2) drawCircle(theme.text, r, Offset(size.width / 2, size.height * (0.2f + 0.3f * i)))
                }
            }

            if (laps.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(formatCs(sw.elapsedCs(now)), style = timeStyle)
                }
            } else {
                Text(formatCs(sw.elapsedCs(now)), style = timeStyle, modifier = Modifier.fillMaxWidth().padding(top = 28.dp))
                Text(
                    formatCs(sw.currentLapCs(now)),
                    style = timeStyle.copy(fontSize = 40.sp, color = theme.textSecondary),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(28.dp))
                LapRow(theme.headerLap, theme.headerLapTime, theme.headerTotal, theme.textSecondary, header = true)
                Box(Modifier.padding(horizontal = 32.dp).fillMaxWidth().height(1.dp).background(Color(0x33000000)))
                LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)) {
                    items(laps.asReversed(), key = { it.number }) { lap ->
                        val color = when (marks[lap.number]) {
                            LapMark.BEST -> theme.lapBest
                            LapMark.WORST -> theme.lapWorst
                            else -> theme.text
                        }
                        LapRow(
                            if (lap.number < 10) "0${lap.number}" else "${lap.number}",
                            formatCs(lap.lapCs), formatCs(lap.totalCs), color, header = false,
                        )
                    }
                }
            }

            // Puntos de página (decorativos)
            Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.Center) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0x55000000)))
                Spacer(Modifier.width(14.dp))
                Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0x99000000)))
            }

            // Botones
            Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                when (phase) {
                    Phase.PAUSED -> {
                        // Pulsación corta: restablecer. Larga: restablecer + armar el forzaje (vibra).
                        CircleButton(
                            theme.resetLabel, theme.buttonSecondaryBg, theme.text,
                            Modifier.pressAndHold(ARM_HOLD_MS, onHold = onArmForcing, onTap = { state.reset() }),
                        )
                    }
                    else -> CircleButton(
                        theme.lapLabel, theme.buttonSecondaryBg,
                        if (phase == Phase.RUNNING) theme.text else theme.textSecondary,
                        if (phase == Phase.RUNNING) Modifier.clickable { state.lap() } else Modifier,
                    )
                }
                when (phase) {
                    Phase.IDLE -> CircleButton(theme.startLabel, theme.buttonPrimaryBg, theme.buttonPrimaryText,
                        Modifier.clickable { state.start() })
                    Phase.RUNNING -> CircleButton(theme.stopLabel, theme.buttonStopBg, theme.buttonStopText,
                        Modifier.clickable { state.stop() })
                    Phase.PAUSED -> CircleButton(theme.resumeLabel, theme.buttonPrimaryBg, theme.buttonPrimaryText,
                        Modifier.clickable { state.start() })
                }
            }

            Spacer(Modifier.height(28.dp))
            BottomNav(theme)
            Spacer(Modifier.height(20.dp))
        }

        // Zona secreta invisible en la esquina superior derecha.
        Box(
            Modifier.align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .size(96.dp)
                .pressAndHold(secretHoldMs, onHold = onSecret),
        )
    }
}

@Composable
private fun CircleButton(label: String, bg: Color, textColor: Color, modifier: Modifier) {
    Box(
        Modifier.size(120.dp).clip(CircleShape).background(bg).then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LapRow(a: String, b: String, c: String, color: Color, header: Boolean) {
    val style = TextStyle(
        fontSize = if (header) 18.sp else 20.sp,
        fontWeight = if (header) FontWeight.Medium else FontWeight.Normal,
        color = color, textAlign = TextAlign.Center, fontFeatureSettings = "tnum",
    )
    Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = if (header) 12.dp else 8.dp)) {
        Text(a, style = style, modifier = Modifier.weight(0.8f))
        Text(b, style = style, modifier = Modifier.weight(1.4f))
        Text(c, style = style, modifier = Modifier.weight(1.4f))
    }
}

/** Barra inferior con las 4 pestañas. Fase 1: solo el cronómetro funciona; las demás no responden al toque. */
@Composable
private fun BottomNav(theme: ClockTheme) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(theme.navBg).padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 0..3) {
                val selected = i == 2
                Box(
                    Modifier.width(if (selected) 84.dp else 70.dp).height(54.dp).clip(RoundedCornerShape(50))
                        .background(if (selected) theme.navSelectedBg else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(26.dp)) { drawNavIcon(i, theme.navIcon) }
                }
            }
        }
    }
}

private fun DrawScope.drawNavIcon(index: Int, c: Color) {
    val w = size.width
    val sw = w * 0.085f
    val line = Stroke(width = sw, cap = StrokeCap.Round)
    when (index) {
        0 -> { // alarma
            drawCircle(c, w * 0.34f, Offset(w / 2, w * 0.55f), style = line)
            drawLine(c, Offset(w / 2, w * 0.55f), Offset(w / 2, w * 0.38f), sw, StrokeCap.Round)
            drawLine(c, Offset(w / 2, w * 0.55f), Offset(w * 0.62f, w * 0.6f), sw, StrokeCap.Round)
            drawLine(c, Offset(w * 0.08f, w * 0.2f), Offset(w * 0.2f, w * 0.08f), sw, StrokeCap.Round)
            drawLine(c, Offset(w * 0.92f, w * 0.2f), Offset(w * 0.8f, w * 0.08f), sw, StrokeCap.Round)
            drawLine(c, Offset(w * 0.25f, w * 0.92f), Offset(w * 0.32f, w * 0.84f), sw, StrokeCap.Round)
            drawLine(c, Offset(w * 0.75f, w * 0.92f), Offset(w * 0.68f, w * 0.84f), sw, StrokeCap.Round)
        }
        1 -> { // reloj mundial (globo)
            drawCircle(c, w * 0.42f, Offset(w / 2, w / 2), style = line)
            drawOval(c, Offset(w * 0.30f, w * 0.08f), Size(w * 0.40f, w * 0.84f), style = line)
            drawLine(c, Offset(w * 0.08f, w / 2), Offset(w * 0.92f, w / 2), sw, StrokeCap.Round)
        }
        2 -> { // cronómetro (seleccionado: relleno)
            drawCircle(c, w * 0.36f, Offset(w / 2, w * 0.58f))
            drawLine(c, Offset(w * 0.38f, w * 0.1f), Offset(w * 0.62f, w * 0.1f), sw, StrokeCap.Round)
            drawLine(c, Offset(w / 2, w * 0.1f), Offset(w / 2, w * 0.2f), sw, StrokeCap.Round)
            drawLine(c, Offset(w * 0.8f, w * 0.26f), Offset(w * 0.88f, w * 0.18f), sw, StrokeCap.Round)
            drawLine(Color.White, Offset(w / 2, w * 0.58f), Offset(w * 0.62f, w * 0.42f), sw * 0.9f, StrokeCap.Round)
        }
        else -> { // temporizador (reloj de arena)
            val p = Path().apply {
                moveTo(w * 0.22f, w * 0.1f); lineTo(w * 0.78f, w * 0.1f)
                lineTo(w * 0.78f, w * 0.26f); lineTo(w * 0.5f, w * 0.5f)
                lineTo(w * 0.78f, w * 0.74f); lineTo(w * 0.78f, w * 0.9f)
                lineTo(w * 0.22f, w * 0.9f); lineTo(w * 0.22f, w * 0.74f)
                lineTo(w * 0.5f, w * 0.5f); lineTo(w * 0.22f, w * 0.26f); close()
            }
            drawPath(p, c, style = line)
        }
    }
}
