package com.chronix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronix.domain.Phase
import com.chronix.domain.formatCs

/** Samsung One UI: fondo degradado lila, botones circulares y barra inferior en píldora. */
@Composable
fun SamsungStopwatch(p: StopwatchScreenParams) {
    val t = p.theme
    val state = p.state
    val sw = p.sw
    val now = p.now
    val phase = p.phase
    val laps = p.laps
    val d = t.decimal
    val timeStyle = digits(54, t.text, FontWeight.SemiBold)

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(t.bgTop, t.bgBottom)))) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            // Menú de tres puntos (decorativo)
            Row(Modifier.fillMaxWidth().height(72.dp).padding(end = 24.dp), horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
                    val r = size.width * 0.075f
                    for (i in 0..2) drawCircle(t.text, r, Offset(size.width / 2, size.height * (0.2f + 0.3f * i)))
                }
            }

            if (laps.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(formatCs(sw.elapsedCs(now), d), style = timeStyle)
                }
            } else {
                Text(formatCs(sw.elapsedCs(now), d), style = timeStyle, modifier = Modifier.fillMaxWidth().padding(top = 28.dp))
                Text(
                    formatCs(sw.currentLapCs(now), d),
                    style = timeStyle.copy(fontSize = 30.sp, color = t.textSecondary),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(28.dp))
                LapRow(t.headerLap, t.headerLapTime, t.headerTotal, t.textSecondary, header = true)
                Box(Modifier.padding(horizontal = 32.dp).fillMaxWidth().height(1.dp).background(Color(0x33000000)))
                LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)) {
                    items(laps.asReversed(), key = { it.number }) { lap ->
                        val color = p.lapColor(lap, t.text)
                        LapRow(pad2(lap.number), formatCs(lap.lapCs, d), formatCs(lap.totalCs, d), color, header = false)
                    }
                }
            }

            // Puntos de página (decorativos)
            Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.Center) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0x55000000)))
                Spacer(Modifier.width(14.dp))
                Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0x99000000)))
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
        SecretCorner(p, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun CircleButton(label: String, bg: Color, textColor: Color, modifier: Modifier) {
    RoundButton(104.dp, bg, modifier) {
        Text(label, color = textColor, fontSize = 17.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LapRow(a: String, b: String, c: String, color: Color, header: Boolean) {
    val style = digits(if (header) 15 else 17, color, if (header) FontWeight.Medium else FontWeight.Normal)
    Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = if (header) 12.dp else 8.dp)) {
        Text(a, style = style, modifier = Modifier.weight(0.8f))
        Text(b, style = style, modifier = Modifier.weight(1.4f))
        Text(c, style = style, modifier = Modifier.weight(1.4f))
    }
}

/** Barra inferior en píldora. Fase 1: solo el cronómetro funciona; las demás no responden al toque. */
@Composable
private fun SamsungNav(t: ClockTheme) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(t.navBg).padding(6.dp),
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
