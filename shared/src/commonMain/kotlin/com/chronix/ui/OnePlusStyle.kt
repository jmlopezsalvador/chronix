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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronix.domain.Phase

/** `m:ss,cc` del contador central. */
private fun mss(cs: Long, d: String): String {
    val s = cs / 100
    return "${s / 60}:${pad2((s % 60).toInt())}$d${pad2((cs % 100).toInt())}"
}

/** Tiempos de la lista de OnePlus: `m ss,cc` (sin dos puntos, como en el original). */
private fun listTime(cs: Long, d: String): String {
    val s = cs / 100
    return "${s / 60} ${pad2((s % 60).toInt())}$d${pad2((cs % 100).toInt())}"
}

/** OnePlus (OxygenOS 12): título grande, aro azul alrededor del contador y botón azul central. */
@Composable
fun OnePlusStopwatch(p: StopwatchScreenParams) {
    val t = p.theme
    val state = p.state
    val sw = p.sw
    val now = p.now
    val phase = p.phase
    val laps = p.laps
    val d = t.decimal

    Box(Modifier.fillMaxSize().background(t.bgTop)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(top = 20.dp), verticalAlignment = Alignment.Top) {
                Spacer(Modifier.weight(1f))
                IconView(IconKind.GEAR, t.text, 26.dp)
            }
            Text(t.title, style = digits(34, t.text, FontWeight.Light, TextAlign.Start),
                modifier = Modifier.padding(start = 28.dp, top = 12.dp))
            Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(270.dp), contentAlignment = Alignment.Center) {
                    ArcRing(sw.elapsedMs(now), laps.lastOrNull()?.let { it.totalCs * 10 }, Modifier.fillMaxSize(),
                        active = t.accent, track = Color(0xFFDADADA), marker = Color(0xFFFF9F0A))
                    Text(mss(sw.elapsedCs(now), d), style = digits(46, t.text, FontWeight.Light), maxLines = 1)
                }
            }
            Spacer(Modifier.height(30.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 30.dp)) {
                Spacer(Modifier.width(34.dp))
                Text(t.headerLapTime, style = digits(13, t.textSecondary, align = TextAlign.Start), modifier = Modifier.weight(1f))
                Text(t.headerTotal, style = digits(13, t.textSecondary, align = TextAlign.End))
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 30.dp).padding(top = 8.dp)) {
                items(laps.asReversed(), key = { it.number }) { lap ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(pad2(lap.number), style = digits(12, t.textSecondary, align = TextAlign.Start), modifier = Modifier.width(34.dp))
                        Text(listTime(lap.lapCs, d), style = digits(18, p.lapColor(lap, t.text), align = TextAlign.Start), modifier = Modifier.weight(1f))
                        Text(listTime(lap.totalCs, d), style = digits(18, t.text, align = TextAlign.End))
                    }
                }
            }
            // Controles: central azul; reiniciar/compartir en pausa, bandera en marcha
            Row(Modifier.fillMaxWidth().padding(bottom = 26.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (phase == Phase.PAUSED) Box(Modifier.size(64.dp).resetGesture(p), contentAlignment = Alignment.Center) {
                        IconView(IconKind.RESET, t.text, 28.dp)
                    }
                }
                RoundButton(72.dp, t.accent, if (phase == Phase.RUNNING) Modifier.clickable { state.stop() } else Modifier.clickable { state.start() }) {
                    IconView(if (phase == Phase.RUNNING) IconKind.PAUSE else IconKind.PLAY, Color.White, 34.dp)
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    when (phase) {
                        Phase.RUNNING -> Box(Modifier.size(64.dp).clickable { state.lap() }, contentAlignment = Alignment.Center) {
                            IconView(IconKind.FLAG, t.text, 28.dp)
                        }
                        Phase.PAUSED -> IconView(IconKind.SHARE, t.text, 26.dp)
                        Phase.IDLE -> IconView(IconKind.FLAG, Color(0xFFBDBDBD), 28.dp)
                    }
                }
            }
            FlatBottomBar(t, labelSize = 11, boldSelected = true, filledSelected = true)
        }
        SecretCorner(p, Modifier.align(Alignment.TopEnd))
    }
}
