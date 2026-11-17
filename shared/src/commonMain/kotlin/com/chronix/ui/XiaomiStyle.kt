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

/** Xiaomi / Redmi / POCO (HyperOS 2): título, contador ligero, lista sencilla y dos botones blancos con iconos azules. */
@Composable
fun XiaomiStopwatch(p: StopwatchScreenParams) {
    val t = p.theme
    val state = p.state
    val phase = p.phase
    val d = t.decimal

    Box(Modifier.fillMaxSize().background(t.bgTop)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(top = 24.dp), verticalAlignment = Alignment.Top) {
                Spacer(Modifier.weight(1f))
                androidx.compose.foundation.Canvas(Modifier.size(20.dp)) {
                    val r = size.width * 0.09f
                    for (i in 0..2) drawCircle(t.text, r, Offset(size.width / 2, size.height * (0.15f + 0.35f * i)))
                }
            }
            Text(t.title, style = digits(34, t.text, FontWeight.Normal, TextAlign.Start), modifier = Modifier.padding(start = 28.dp, top = 8.dp))
            Spacer(Modifier.height(40.dp))
            Text(formatCs(p.sw.elapsedCs(p.now), d), style = digits(56, t.text, FontWeight.Light),
                modifier = Modifier.fillMaxWidth(), maxLines = 1)
            Spacer(Modifier.height(34.dp))
            LazyColumn(state = rememberLapListState(p.laps.size), modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 30.dp)) {
                items(p.laps.asReversed(), key = { it.number }) { lap ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(pad2(lap.number), style = digits(19, t.textSecondary, align = TextAlign.Start), modifier = Modifier.weight(0.6f))
                        Text("+ " + formatCs(lap.lapCs, d), style = digits(19, t.textSecondary), modifier = Modifier.weight(1.3f))
                        Text(formatCs(lap.totalCs, d), style = digits(19, t.text, FontWeight.Medium, TextAlign.End), modifier = Modifier.weight(1f))
                    }
                }
            }
            // Dos botones: en marcha bandera + pausa; en pausa cuadrado (restablecer) + continuar
            Row(Modifier.fillMaxWidth().padding(bottom = 36.dp), horizontalArrangement = Arrangement.Center) {
                when (phase) {
                    Phase.PAUSED -> Round(Modifier.resetGesture(p)) { IconView(IconKind.STOP, t.accent, 30.dp) }
                    Phase.RUNNING -> Round(Modifier.clickable { state.lap() }) { IconView(IconKind.FLAG, t.accent, 30.dp, filled = true) }
                    Phase.IDLE -> Round(Modifier) { IconView(IconKind.FLAG, Color(0xFFBDBDBD), 30.dp, filled = true) }
                }
                Spacer(Modifier.width(56.dp))
                Round(if (phase == Phase.RUNNING) Modifier.clickable { state.stop() } else Modifier.clickable { state.start() }) {
                    IconView(if (phase == Phase.RUNNING) IconKind.PAUSE else IconKind.PLAY, t.accent, 30.dp)
                }
            }
            FlatBottomBar(t, labelSize = 11, boldSelected = true)
        }
    }
}

@Composable
private fun Round(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    RoundButton(72.dp, Color.White, modifier, elevation = 3.dp, content = content)
}
