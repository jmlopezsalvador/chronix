package com.chronix.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Iconos dibujados con Canvas (no se usa material-icons-extended). */
enum class IconKind { ALARM, WORLD, STOPWATCH, HOURGLASS, CLOCK, BED, PLAY, PAUSE, FLAG, RESET, STOP, SHARE, GEAR }

@Composable
fun IconView(kind: IconKind, color: Color, size: Dp = 26.dp, filled: Boolean = false, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { drawIcon(kind, color, filled) }
}

fun DrawScope.drawIcon(kind: IconKind, c: Color, filled: Boolean = false) {
    val w = size.width
    val sw = w * 0.085f
    val line = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun ln(x1: Float, y1: Float, x2: Float, y2: Float, wd: Float = sw) =
        drawLine(c, Offset(w * x1, w * y1), Offset(w * x2, w * y2), wd, StrokeCap.Round)
    when (kind) {
        IconKind.ALARM -> {
            drawCircle(c, w * 0.34f, Offset(w / 2, w * 0.55f), style = line)
            ln(0.5f, 0.55f, 0.5f, 0.38f); ln(0.5f, 0.55f, 0.62f, 0.6f)
            ln(0.08f, 0.2f, 0.2f, 0.08f); ln(0.92f, 0.2f, 0.8f, 0.08f)
            ln(0.25f, 0.92f, 0.32f, 0.84f); ln(0.75f, 0.92f, 0.68f, 0.84f)
        }
        IconKind.WORLD -> {
            drawCircle(c, w * 0.42f, Offset(w / 2, w / 2), style = line)
            drawOval(c, Offset(w * 0.30f, w * 0.08f), Size(w * 0.40f, w * 0.84f), style = line)
            ln(0.08f, 0.5f, 0.92f, 0.5f)
        }
        IconKind.STOPWATCH -> {
            val center = Offset(w / 2, w * 0.58f)
            if (filled) drawCircle(c, w * 0.36f, center) else drawCircle(c, w * 0.34f, center, style = line)
            ln(0.38f, 0.1f, 0.62f, 0.1f); ln(0.5f, 0.1f, 0.5f, 0.2f); ln(0.8f, 0.26f, 0.88f, 0.18f)
            val handColor = if (filled) Color.White else c
            drawLine(handColor, center, Offset(w * 0.62f, w * 0.42f), sw * 0.9f, StrokeCap.Round)
        }
        IconKind.HOURGLASS -> {
            val p = Path().apply {
                moveTo(w * 0.22f, w * 0.1f); lineTo(w * 0.78f, w * 0.1f)
                lineTo(w * 0.78f, w * 0.26f); lineTo(w * 0.5f, w * 0.5f)
                lineTo(w * 0.78f, w * 0.74f); lineTo(w * 0.78f, w * 0.9f)
                lineTo(w * 0.22f, w * 0.9f); lineTo(w * 0.22f, w * 0.74f)
                lineTo(w * 0.5f, w * 0.5f); lineTo(w * 0.22f, w * 0.26f); close()
            }
            if (filled) drawPath(p, c) else drawPath(p, c, style = line)
        }
        IconKind.CLOCK -> {
            if (filled) drawCircle(c, w * 0.42f, Offset(w / 2, w / 2))
            else drawCircle(c, w * 0.4f, Offset(w / 2, w / 2), style = line)
            val hands = if (filled) Color.White else c
            drawLine(hands, Offset(w / 2, w / 2), Offset(w / 2, w * 0.26f), sw, StrokeCap.Round)
            drawLine(hands, Offset(w / 2, w / 2), Offset(w * 0.66f, w * 0.6f), sw, StrokeCap.Round)
        }
        IconKind.BED -> {
            ln(0.08f, 0.2f, 0.08f, 0.82f); ln(0.08f, 0.66f, 0.92f, 0.66f); ln(0.92f, 0.66f, 0.92f, 0.82f)
            drawCircle(c, w * 0.09f, Offset(w * 0.24f, w * 0.44f), style = line)
            val p = Path().apply { moveTo(w * 0.4f, w * 0.4f); lineTo(w * 0.92f, w * 0.4f); lineTo(w * 0.92f, w * 0.66f); lineTo(w * 0.4f, w * 0.66f); close() }
            drawPath(p, c, style = line)
        }
        IconKind.PLAY -> {
            val p = Path().apply { moveTo(w * 0.3f, w * 0.18f); lineTo(w * 0.82f, w * 0.5f); lineTo(w * 0.3f, w * 0.82f); close() }
            drawPath(p, c)
        }
        IconKind.PAUSE -> {
            drawRoundRect(c, Offset(w * 0.25f, w * 0.2f), Size(w * 0.17f, w * 0.6f), CornerRadius(w * 0.03f))
            drawRoundRect(c, Offset(w * 0.58f, w * 0.2f), Size(w * 0.17f, w * 0.6f), CornerRadius(w * 0.03f))
        }
        IconKind.FLAG -> {
            ln(0.28f, 0.14f, 0.28f, 0.88f, sw * 1.1f)
            val p = Path().apply { moveTo(w * 0.28f, w * 0.16f); lineTo(w * 0.84f, w * 0.38f); lineTo(w * 0.28f, w * 0.6f); close() }
            if (filled) drawPath(p, c) else drawPath(p, c, style = line)
        }
        IconKind.RESET -> {
            drawArc(c, startAngle = -50f, sweepAngle = 290f, useCenter = false,
                topLeft = Offset(w * 0.16f, w * 0.18f), size = Size(w * 0.68f, w * 0.68f), style = line)
            val p = Path().apply { moveTo(w * 0.54f, w * 0.02f); lineTo(w * 0.7f, w * 0.18f); lineTo(w * 0.5f, w * 0.28f) }
            drawPath(p, c, style = line)
        }
        IconKind.STOP -> drawRoundRect(c, Offset(w * 0.22f, w * 0.22f), Size(w * 0.56f, w * 0.56f), CornerRadius(w * 0.08f))
        IconKind.SHARE -> {
            val a = Offset(w * 0.25f, w * 0.5f); val b = Offset(w * 0.75f, w * 0.22f); val d = Offset(w * 0.75f, w * 0.78f)
            drawLine(c, a, b, sw, StrokeCap.Round); drawLine(c, a, d, sw, StrokeCap.Round)
            drawCircle(c, w * 0.1f, a); drawCircle(c, w * 0.1f, b); drawCircle(c, w * 0.1f, d)
        }
        IconKind.GEAR -> {
            drawCircle(c, w * 0.2f, Offset(w / 2, w / 2), style = line)
            drawCircle(c, w * 0.34f, Offset(w / 2, w / 2), style = Stroke(width = w * 0.12f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(w * 0.1f, w * 0.17f))))
        }
    }
}
