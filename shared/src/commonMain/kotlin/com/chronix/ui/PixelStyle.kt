package com.chronix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronix.domain.Phase
import com.chronix.domain.formatCs

/** Google Pixel (Clock 8.1, Material 3 Expressive, oscuro): cifras cian, tarjetas de vueltas y tres botones apilados. */
@Composable
fun PixelStopwatch(p: StopwatchScreenParams) {
    val t = p.theme
    val state = p.state
    val sw = p.sw
    val phase = p.phase
    val laps = p.laps
    val listState = rememberLazyListState()
    LaunchedEffect(laps.size) { if (laps.isNotEmpty()) listState.animateScrollToItem(laps.size - 1) }

    Box(Modifier.fillMaxSize().background(t.bgTop)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(t.title, color = t.text, fontSize = 22.sp, modifier = Modifier.weight(1f))
                androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
                    val r = size.width * 0.075f
                    for (i in 0..2) drawCircle(t.text, r, androidx.compose.ui.geometry.Offset(size.width / 2, size.height * (0.2f + 0.3f * i)))
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(formatCs(sw.elapsedCs(p.now), t.decimal), style = digits(76, t.text, FontWeight.Medium), maxLines = 1)
            }
            LazyRow(
                Modifier.fillMaxWidth().height(138.dp), state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                itemsIndexed(laps, key = { _, lap -> lap.number }) { _, lap ->
                    val col = p.lapColor(lap, t.text)
                    Column(
                        Modifier.width(138.dp).height(124.dp).clip(RoundedCornerShape(28.dp))
                            .border(2.dp, Color(0xFF123C4A), RoundedCornerShape(28.dp)),
                        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(pad2(lap.number), style = digits(16, col))
                        Text(formatCs(lap.lapCs, t.decimal), style = digits(17, col), modifier = Modifier.padding(top = 4.dp))
                        Text(formatCs(lap.totalCs, t.decimal), style = digits(17, col), modifier = Modifier.padding(top = 14.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (phase) {
                    Phase.RUNNING -> Pill(t.stopLabel, t.stopBg, t.stopText, 108, Modifier.clickable { state.stop() })
                    Phase.IDLE -> Pill(t.startLabel, t.accent, t.accentOn, 108, Modifier.clickable { state.start() })
                    Phase.PAUSED -> Pill(t.resumeLabel, t.accent, t.accentOn, 108, Modifier.clickable { state.start() })
                }
                // Restablecer siempre visible: pulsación corta restablece, larga arma el forzaje
                Pill(t.resetLabel, t.buttonBg, t.buttonText, 72, Modifier.resetGesture(p))
                Pill(t.lapLabel, t.buttonBg, if (phase == Phase.RUNNING) t.buttonText else t.textSecondary, 72,
                    if (phase == Phase.RUNNING) Modifier.clickable { state.lap() } else Modifier)
            }
            Spacer(Modifier.height(12.dp))
            PillBottomBar(t)
        }
        SecretCorner(p, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun Pill(label: String, bg: Color, color: Color, height: Int, modifier: Modifier) {
    Box(
        Modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape(if (height > 90) 36.dp else 50.dp)).background(bg).then(modifier),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = color, fontSize = 22.sp) }
}
