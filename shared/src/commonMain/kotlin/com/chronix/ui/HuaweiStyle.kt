package com.chronix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronix.domain.Phase
import com.chronix.domain.formatCs

/** Huawei (EMUI 12): esfera analógica con subesfera de minutos, total digital debajo y vueltas en tarjetas blancas. */
@Composable
fun HuaweiStopwatch(p: StopwatchScreenParams) {
    val t = p.theme
    val state = p.state
    val sw = p.sw
    val now = p.now
    val phase = p.phase
    val laps = p.laps
    val d = t.decimal

    Box(Modifier.fillMaxSize().background(t.bgTop)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Text(t.title, style = digits(30, t.text, FontWeight.Medium, TextAlign.Start),
                modifier = Modifier.padding(start = 24.dp, top = 28.dp))
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                ClassicDial(sw.elapsedMs(now), 0L, Modifier.size(300.dp), iphone = false)
            }
            Text(formatCs(sw.elapsedCs(now), d), style = digits(24, t.text, FontWeight.Medium), modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            Text(
                if (laps.isNotEmpty()) formatCs(sw.currentLapCs(now), d) else " ",
                style = digits(17, t.textSecondary), modifier = Modifier.fillMaxWidth(),
            )
            LazyColumn(state = rememberLapListState(p.laps.size), modifier = 
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp).padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(laps.asReversed(), key = { it.number }) { lap ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(t.surface).padding(horizontal = 18.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(pad2(lap.number), style = digits(19, t.text, align = TextAlign.Start), modifier = Modifier.weight(0.6f))
                        if (p.showTotals) {
                            Text("+" + formatCs(lap.lapCs, d), style = digits(17, t.textSecondary), modifier = Modifier.weight(1.3f))
                            Text(formatCs(lap.totalCs, d), style = digits(19, t.text, FontWeight.Medium, TextAlign.End), modifier = Modifier.weight(1f))
                        } else {
                            Text(formatCs(lap.lapCs, d), style = digits(19, t.text, FontWeight.Medium, TextAlign.End), modifier = Modifier.weight(2.3f))
                        }
                    }
                }
            }
            // Controles: reiniciar (blanco), pausa/continuar (azul), vuelta (blanco)
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                RoundButton(58.dp, if (phase == Phase.PAUSED) Color.White else Color(0xFFF7F8FA),
                    if (phase == Phase.PAUSED) Modifier.resetGesture(p) else Modifier) {
                    IconView(IconKind.RESET, if (phase == Phase.PAUSED) t.text else Color(0xFFB4B6BC), 26.dp)
                }
                Spacer(Modifier.width(14.dp))
                RoundButton(76.dp, t.accent, if (phase == Phase.RUNNING) Modifier.clickable { state.stop() } else Modifier.clickable { state.start() }, elevation = 6.dp) {
                    IconView(if (phase == Phase.RUNNING) IconKind.PAUSE else IconKind.PLAY, Color.White, 34.dp)
                }
                Spacer(Modifier.width(14.dp))
                RoundButton(58.dp, if (phase == Phase.RUNNING) Color.White else Color(0xFFF7F8FA),
                    if (phase == Phase.RUNNING) Modifier.clickable { state.lap() } else Modifier) {
                    IconView(IconKind.STOPWATCH, if (phase == Phase.RUNNING) t.text else Color(0xFFB4B6BC), 26.dp)
                }
            }
            FlatBottomBar(t, labelSize = 11, boldSelected = false)
        }
    }
}
