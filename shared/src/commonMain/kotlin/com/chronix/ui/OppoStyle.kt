package com.chronix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronix.domain.Phase
import com.chronix.domain.formatCs

/** OPPO (ColorOS 13): esfera blanca con relieve, cifras con los segundos en azul y botón azul con sombra. */
@Composable
fun OppoStopwatch(p: StopwatchScreenParams) {
    val t = p.theme
    val state = p.state
    val sw = p.sw
    val now = p.now
    val phase = p.phase
    val laps = p.laps
    val d = t.decimal
    val time = formatCs(sw.elapsedCs(now), d)
    val minutes = time.substringBefore(":") + ":"
    val rest = time.substringAfter(":")

    Box(Modifier.fillMaxSize().background(t.bgTop)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(end = 28.dp), horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
                    val r = size.width * 0.075f
                    for (i in 0..2) drawCircle(t.text, r, Offset(size.width / 2, size.height * (0.2f + 0.3f * i)))
                }
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(320.dp), contentAlignment = Alignment.Center) {
                    DottedDial(sw.elapsedMs(now), Modifier.fillMaxSize(), t.accent)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row {
                            Text(minutes, style = digits(44, t.text, FontWeight.SemiBold))
                            Text(rest, style = digits(44, t.accent, FontWeight.SemiBold))
                        }
                        if (laps.isNotEmpty()) {
                            Text(formatCs(sw.currentLapCs(now), d), style = digits(18, t.textSecondary), modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
            if (laps.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 30.dp)) {
                    Text(t.headerLap, style = digits(15, t.textSecondary, align = TextAlign.Start), modifier = Modifier.weight(0.7f))
                    Text(t.headerLapTime, style = digits(15, t.textSecondary, align = if (p.showTotals) TextAlign.Center else TextAlign.End), modifier = Modifier.weight(if (p.showTotals) 1.3f else 2.3f))
                    if (p.showTotals) Text(t.headerTotal, style = digits(15, t.textSecondary, align = TextAlign.End), modifier = Modifier.weight(1f))
                }
            }
            LazyColumn(state = rememberLapListState(p.laps.size), modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 30.dp).padding(top = 10.dp)) {
                items(laps.asReversed(), key = { it.number }) { lap ->
                    val c = p.lapColor(lap, t.text)
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Text(pad2(lap.number), style = digits(19, c, align = TextAlign.Start), modifier = Modifier.weight(0.7f))
                        Text("+" + formatCs(lap.lapCs, d), style = digits(19, c, align = if (p.showTotals) TextAlign.Center else TextAlign.End), modifier = Modifier.weight(if (p.showTotals) 1.3f else 2.3f))
                        if (p.showTotals) Text(formatCs(lap.totalCs, d), style = digits(19, t.text, align = TextAlign.End), modifier = Modifier.weight(1f))
                    }
                }
            }
            // Controles: en cero solo el triángulo; después reiniciar (gris en marcha), pausa/continuar y bandera
            Row(Modifier.fillMaxWidth().padding(bottom = 26.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (phase != Phase.IDLE) {
                        Box(
                            if (phase == Phase.PAUSED) Modifier.size(64.dp).resetGesture(p) else Modifier.size(64.dp),
                            contentAlignment = Alignment.Center,
                        ) { IconView(IconKind.RESET, if (phase == Phase.PAUSED) t.text else Color(0xFFB9B9BE), 28.dp) }
                    }
                }
                RoundButton(
                    76.dp, t.accent,
                    if (phase == Phase.RUNNING) Modifier.clickable { state.stop() } else Modifier.clickable { state.start() },
                    elevation = 10.dp,
                ) { IconView(if (phase == Phase.RUNNING) IconKind.PAUSE else IconKind.PLAY, Color.White, 36.dp) }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (phase != Phase.IDLE) {
                        Box(
                            if (phase == Phase.RUNNING) Modifier.size(64.dp).blocksSecret().clickable { state.lap() } else Modifier.size(64.dp),
                            contentAlignment = Alignment.Center,
                        ) { IconView(IconKind.FLAG, if (phase == Phase.RUNNING) t.text else Color(0xFFB9B9BE), 28.dp) }
                    }
                }
            }
            FlatBottomBar(t, labelSize = 12, boldSelected = true, filledSelected = true)
        }
    }
}
