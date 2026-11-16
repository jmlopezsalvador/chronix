package com.chronix.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Una vuelta de la esfera = 60 s. */
private const val REV_MS = 60_000f

private fun polar(c: Offset, r: Float, deg: Float): Offset {
    val a = (deg - 90f) * (PI.toFloat() / 180f)
    return Offset(c.x + r * cos(a), c.y + r * sin(a))
}

private fun DrawScope.centeredText(tm: TextMeasurer, text: String, at: Offset, sizePx: Float, color: Color, weight: FontWeight = FontWeight.Normal) {
    val layout = tm.measure(text, TextStyle(fontSize = sizePx.toSp(), color = color, fontWeight = weight))
    drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
}

/**
 * Esfera analógica de cronómetro: aguja de segundos, subesfera de minutos (30) y, opcionalmente, aguja de vuelta.
 * `iphone`: negra, 240 marcas finas, aguja naranja y aguja azul de vuelta. Si no: clara con volumen (Huawei).
 */
@Composable
fun ClassicDial(
    elapsedMs: Long,
    lapMs: Long,
    modifier: Modifier = Modifier,
    iphone: Boolean,
    centerText: String? = null,
) {
    val tm = rememberTextMeasurer()
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = minOf(size.width, size.height) / 2f * 0.98f
        val numberColor = if (iphone) Color.White else Color(0xFF111111)
        val tickColor = if (iphone) Color(0xFF8E8E93) else Color(0xFFB9BEC8)
        val hand = if (iphone) Color(0xFFFF9F0A) else Color(0xFFE8531A)

        if (!iphone) {
            // Cuerpo con relieve suave
            drawCircle(Brush.radialGradient(listOf(Color(0x22000000), Color.Transparent), c + Offset(0f, r * 0.06f), r * 1.06f), r * 1.06f, c + Offset(0f, r * 0.06f))
            drawCircle(Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE7EBF5)), c.y - r, c.y + r), r, c)
        }
        // Marcas
        val ticks = if (iphone) 240 else 60
        for (i in 0 until ticks) {
            val deg = i * 360f / ticks
            val big = i % (ticks / 12) == 0
            val len = if (big) r * 0.075f else r * 0.035f
            drawLine(tickColor, polar(c, r * 0.97f, deg), polar(c, r * 0.97f - len, deg), if (big) r * 0.012f else r * 0.006f, StrokeCap.Butt)
        }
        // Números
        for (k in 0 until 12) {
            val n = if (k == 0) 60 else k * 5
            centeredText(tm, "$n", polar(c, r * 0.80f, k * 30f), r * 0.115f, numberColor, if (iphone) FontWeight.Medium else FontWeight.Normal)
        }
        // Subesfera de minutos
        val sc = c + Offset(0f, -r * 0.36f)
        val sr = r * 0.2f
        drawCircle(if (iphone) Color(0xFF8E8E93) else Color(0xFFB9BEC8), sr, sc, style = Stroke(r * 0.006f))
        for (k in 0 until 6) {
            val n = if (k == 0) 30 else k * 5
            centeredText(tm, "$n", polar(sc, sr * 0.68f, k * 60f), sr * 0.22f, numberColor)
        }
        val minDeg = (elapsedMs / 60_000f % 30f) / 30f * 360f
        drawLine(hand, sc, polar(sc, sr * 0.85f, minDeg), r * 0.012f, StrokeCap.Round)
        drawCircle(hand, r * 0.016f, sc)

        // Texto digital en el centro (iPhone)
        if (centerText != null) centeredText(tm, centerText, c + Offset(0f, r * 0.32f), r * 0.12f, numberColor)

        // Aguja de vuelta (azul, iPhone)
        if (iphone) {
            val lapDeg = lapMs % 60_000L / REV_MS * 360f
            drawLine(Color(0xFF0A84FF), polar(c, -r * 0.22f, lapDeg), polar(c, r * 0.97f, lapDeg), r * 0.012f, StrokeCap.Round)
        }
        // Aguja de segundos
        val deg = elapsedMs % 60_000L / REV_MS * 360f
        drawLine(hand, polar(c, -r * 0.18f, deg), polar(c, r * 0.9f, deg), r * 0.014f, StrokeCap.Round)
        drawCircle(Color(0xFFF1F3F5).takeIf { !iphone } ?: Color.Black, r * 0.03f, c)
        drawCircle(hand, r * 0.03f, c, style = Stroke(r * 0.012f))
    }
}

/** Aro de OnePlus: aro gris con un arco azul que avanza y un punto al final; marcas finas por dentro. */
@Composable
fun ArcRing(elapsedMs: Long, markerMs: Long?, modifier: Modifier = Modifier, active: Color, track: Color, marker: Color) {
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val stroke = size.width * 0.028f
        val r = size.width / 2 - stroke
        drawCircle(track, r, c, style = Stroke(stroke))
        for (i in 0 until 60) {
            val deg = i * 6f
            drawLine(track, polar(c, r * 0.93f, deg), polar(c, r * 0.88f, deg), 2f)
        }
        val sweep = elapsedMs % 60_000L / REV_MS * 360f
        drawArc(active, -90f, if (elapsedMs > 0 && sweep == 0f) 360f else sweep, false,
            Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(stroke, cap = StrokeCap.Round))
        if (elapsedMs > 0) drawCircle(active, stroke * 1.5f, polar(c, r, sweep))
        if (markerMs != null) {
            val m = polar(c, r * 0.8f, markerMs % 60_000L / REV_MS * 360f)
            val p = Path().apply {
                moveTo(m.x - stroke * 0.7f, m.y - stroke * 0.7f); lineTo(m.x + stroke * 0.7f, m.y); lineTo(m.x - stroke * 0.7f, m.y + stroke * 0.7f); close()
            }
            drawPath(p, marker)
        }
    }
}

/** Esfera de OPPO: disco blanco con resplandor azul violeta y 60 puntos; un punto azul con estela recorre el borde. */
@Composable
fun DottedDial(elapsedMs: Long, modifier: Modifier = Modifier, accent: Color) {
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.width / 2 * 0.86f
        drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.28f), Color.Transparent), c + Offset(-r * 0.15f, r * 0.22f), r * 1.18f), r * 1.18f, c + Offset(-r * 0.15f, r * 0.22f))
        drawCircle(Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF1F1F6)), c.y - r, c.y + r), r, c)
        val cur = elapsedMs % 60_000L / REV_MS * 60f
        for (i in 0 until 60) {
            val behind = ((cur - i) % 60f + 60f) % 60f
            val trail = elapsedMs > 0 && behind < 5f
            val col = if (trail) accent.copy(alpha = 1f - behind / 6f) else Color(0xFFB4B4BA)
            drawCircle(col, if (trail) r * 0.016f else r * 0.008f, polar(c, r * 0.86f, i * 6f))
        }
        val head = polar(c, r * 0.86f, cur * 6f)
        drawCircle(accent, r * 0.026f, head)
    }
}
