package com.chronix.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronix.domain.Phase
import com.chronix.domain.formatCs

/**
 * Samsung One UI: fondo degradado lila, botones circulares y barra inferior en píldora.
 * Dos páginas deslizables (los dos puntos): analógica (primer punto) y digital (segundo, la de inicio).
 */
@Composable
fun SamsungStopwatch(p: StopwatchScreenParams) {
    val t = p.theme
    val state = p.state
    val phase = p.phase
    val laps = p.laps
    val pager = rememberPagerState(initialPage = 1, pageCount = { 2 })

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(t.bgTop, t.bgBottom)))) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            // Barra superior: «copiar vueltas» (solo con vueltas) y menú de tres puntos, ambos decorativos
            Row(Modifier.fillMaxWidth().height(72.dp).padding(end = 24.dp), horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically) {
                if (laps.isNotEmpty()) {
                    Canvas(Modifier.size(26.dp)) {
                        val w = size.width
                        val st = Stroke(w * 0.09f)
                        drawRoundRect(t.text, Offset(w * 0.3f, w * 0.08f), Size(w * 0.6f, w * 0.6f), CornerRadius(w * 0.12f), style = st)
                        drawRoundRect(t.text, Offset(w * 0.1f, w * 0.3f), Size(w * 0.6f, w * 0.6f), CornerRadius(w * 0.12f), style = st)
                    }
                    Spacer(Modifier.width(22.dp))
                }
                Canvas(Modifier.size(24.dp)) {
                    val r = size.width * 0.075f
                    for (i in 0..2) drawCircle(t.text, r, Offset(size.width / 2, size.height * (0.2f + 0.3f * i)))
                }
            }

            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page ->
                if (page == 0) AnalogPage(p) else DigitalPage(p)
            }

            // Puntos de página: el activo es más oscuro
            Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.Center) {
                for (i in 0..1) {
                    if (i > 0) Spacer(Modifier.width(14.dp))
                    Box(Modifier.size(9.dp).clip(CircleShape).background(if (pager.currentPage == i) Color(0x99000000) else Color(0x44000000)))
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                when (phase) {
                    Phase.PAUSED -> CircleButton(t.resetLabel, t.buttonBg, t.buttonText, Modifier.resetGesture(p))
                    else -> CircleButton(
                        t.lapLabel, t.buttonBg,
                        if (phase == Phase.RUNNING) t.buttonText else t.textSecondary,
                        if (phase == Phase.RUNNING) Modifier.clickable { state.lap() } else Modifier,
                    )
                }
                when (phase) {
                    Phase.IDLE -> CircleButton(t.startLabel, t.accent, t.accentOn, Modifier.clickable { state.start() })
                    Phase.RUNNING -> CircleButton(t.stopLabel, t.stopBg, t.stopText, Modifier.clickable { state.stop() })
                    Phase.PAUSED -> CircleButton(t.resumeLabel, t.accent, t.accentOn, Modifier.clickable { state.start() })
                }
            }

            Spacer(Modifier.height(28.dp))
            SamsungNav(t)
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** Página digital: total grande, vuelta en curso en gris y tabla. */
@Composable
private fun DigitalPage(p: StopwatchScreenParams) {
    val t = p.theme
    val sw = p.sw
    val now = p.now
    val d = t.decimal
    val timeStyle = digits(54, t.text, FontWeight.SemiBold)
    if (p.laps.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(formatCs(sw.elapsedCs(now), d), style = timeStyle)
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Text(formatCs(sw.elapsedCs(now), d), style = timeStyle, modifier = Modifier.fillMaxWidth().padding(top = 28.dp))
            Text(
                formatCs(sw.currentLapCs(now), d),
                style = timeStyle.copy(fontSize = 30.sp, color = t.textSecondary),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(28.dp))
            LapTable(p, Modifier.weight(1f))
        }
    }
}

/** Página analógica: esfera con marcas y dos agujas, el total digital en el centro y la tabla debajo. */
@Composable
private fun AnalogPage(p: StopwatchScreenParams) {
    val t = p.theme
    val sw = p.sw
    val now = p.now
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(4.dp))
        SamsungDial(
            sw.elapsedMs(now), sw.currentLapCs(now) * 10, p.laps.isNotEmpty(),
            Modifier.fillMaxWidth(0.8f).aspectRatio(1f), hand = Color(0xFF5B5BD6),
        ) {
            Text(formatCs(sw.elapsedCs(now), t.decimal), style = digits(36, t.text, FontWeight.Bold), maxLines = 1)
        }
        Spacer(Modifier.height(10.dp))
        LapTable(p, Modifier.weight(1f))
    }
}

/** Cabecera y tabla de vueltas (la última arriba). */
@Composable
private fun LapTable(p: StopwatchScreenParams, modifier: Modifier) {
    val t = p.theme
    val d = t.decimal
    Column(modifier.fillMaxWidth()) {
        if (p.laps.isNotEmpty()) {
            LapRow(t.headerLap, t.headerLapTime, if (p.showTotals) t.headerTotal else null, t.textSecondary, header = true)
            Box(Modifier.padding(horizontal = 32.dp).fillMaxWidth().height(1.dp).background(Color(0x33000000)))
        }
        LazyColumn(state = rememberLapListState(p.laps.size), modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 6.dp)) {
            items(p.laps.asReversed(), key = { it.number }) { lap ->
                LapRow(pad2(lap.number), formatCs(lap.lapCs, d), if (p.showTotals) formatCs(lap.totalCs, d) else null, p.lapColor(lap, t.text), header = false)
            }
        }
    }
}

@Composable
private fun CircleButton(label: String, bg: Color, textColor: Color, modifier: Modifier) {
    RoundButton(104.dp, bg, modifier) {
        Text(label, color = textColor, fontSize = 17.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LapRow(a: String, b: String, c: String?, color: Color, header: Boolean) {
    val style = digits(if (header) 15 else 17, color, if (header) FontWeight.Medium else FontWeight.Normal)
    Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = if (header) 12.dp else 8.dp)) {
        Text(a, style = style, modifier = Modifier.weight(0.8f))
        // Sin totales la columna de vuelta ocupa el hueco de la de totales
        Text(b, style = style, modifier = Modifier.weight(if (c != null) 1.4f else 2.8f))
        if (c != null) Text(c, style = style, modifier = Modifier.weight(1.4f))
    }
}

/** Barra inferior en píldora. Fase 1: solo el cronómetro funciona; las demás no responden al toque. */
@Composable
private fun SamsungNav(t: ClockTheme) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(t.navBg).blocksSecret().padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            t.tabs.forEachIndexed { i, tab ->
                val selected = i == t.selectedTab
                Box(
                    Modifier.width(if (selected) 84.dp else 70.dp).height(54.dp).clip(RoundedCornerShape(50))
                        .background(if (selected) t.navSelectedBg else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) { IconView(tab.icon, t.navIcon, 26.dp, filled = selected) }
            }
        }
    }
}
