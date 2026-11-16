package com.chronix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronix.domain.Phase
import com.chronix.domain.formatCs

/** iPhone (iOS 17): negro, cifras finas, botones circulares y dos páginas (digital / analógica) deslizables. */
@Composable
fun IPhoneStopwatch(p: StopwatchScreenParams) {
    val t = p.theme
    val state = p.state
    val sw = p.sw
    val now = p.now
    val phase = p.phase
    val laps = p.laps
    val pager = rememberPagerState(pageCount = { 2 })
    val time = formatCs(sw.elapsedCs(now), t.decimal)

    Box(Modifier.fillMaxSize().background(t.bgTop)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Spacer(Modifier.height(24.dp))
            HorizontalPager(pager, Modifier.fillMaxWidth().aspectRatio(1f)) { page ->
                if (page == 0) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(time, style = digits(80, t.text, FontWeight.ExtraLight), maxLines = 1)
                    }
                } else {
                    ClassicDial(
                        sw.elapsedMs(now), sw.currentLapCs(now) * 10, Modifier.fillMaxSize().padding(8.dp),
                        iphone = true, centerText = time,
                    )
                }
            }
            // Botones con los puntos de página en medio
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                when (phase) {
                    Phase.PAUSED -> Round(t.buttonBg, t.resetLabel, t.buttonText, Modifier.resetGesture(p))
                    Phase.RUNNING -> Round(t.buttonBg, t.lapLabel, t.buttonText, Modifier.clickable { state.lap() })
                    Phase.IDLE -> Round(Color(0xFF1C1C1E), t.lapLabel, Color(0xFF5A5A5E), Modifier)
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                    for (i in 0..1) {
                        Box(Modifier.padding(horizontal = 5.dp).size(8.dp).clip(CircleShape)
                            .background(if (pager.currentPage == i) Color.White else Color(0xFF5A5A5E)))
                    }
                }
                when (phase) {
                    Phase.RUNNING -> Round(t.stopBg, t.stopLabel, t.stopText, Modifier.clickable { state.stop() })
                    else -> Round(t.accent, if (phase == Phase.IDLE) t.startLabel else t.resumeLabel, t.accentOn,
                        Modifier.clickable { state.start() })
                }
            }
            Spacer(Modifier.height(18.dp))
            // Lista: fila de la vuelta en curso arriba, y las vueltas hechas debajo
            LazyColumn(state = rememberLapListState(p.laps.size), modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp)) {
                if (phase != Phase.IDLE) {
                    item { Row1("${t.lapLabel} ${laps.size + 1}", formatCs(sw.currentLapCs(now), t.decimal), t.text) }
                }
                items(laps.asReversed(), key = { it.number }) { lap ->
                    Row1("${t.lapLabel} ${lap.number}", formatCs(lap.lapCs, t.decimal), p.lapColor(lap, t.text))
                }
            }
            FlatBottomBar(t, labelSize = 10)
        }
        SecretCorner(p, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun Round(bg: Color, label: String, color: Color, modifier: Modifier) {
    RoundButton(84.dp, bg, modifier, border = bg.copy(alpha = 0.55f)) {
        Text(label, color = color, fontSize = 17.sp)
    }
}

@Composable
private fun Row1(left: String, right: String, color: Color) {
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2C2E)))
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(left, style = digits(17, color, align = androidx.compose.ui.text.style.TextAlign.Start))
            Text(right, style = digits(17, color, align = androidx.compose.ui.text.style.TextAlign.End))
        }
    }
}
