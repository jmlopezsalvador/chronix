package com.chronix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Botón circular genérico. `modifier` lleva el gesto (clickable / resetGesture). */
@Composable
fun RoundButton(
    size: Dp,
    bg: Color,
    modifier: Modifier = Modifier,
    border: Color? = null,
    elevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    var m = Modifier.size(size)
    if (elevation > 0.dp) m = m.shadow(elevation, CircleShape)
    m = m.clip(CircleShape).background(bg).blocksSecret()
    if (border != null) m = m.border(2.dp, border, CircleShape)
    Box(m.then(modifier), contentAlignment = Alignment.Center, content = content)
}

/** Estilo de cifras con anchura fija (los dígitos no bailan al cambiar). */
fun digits(size: Int, color: Color, weight: FontWeight = FontWeight.Normal, align: TextAlign = TextAlign.Center) =
    TextStyle(fontSize = size.sp, color = color, fontWeight = weight, textAlign = align, fontFeatureSettings = "tnum")

fun pad2(n: Int) = if (n < 10) "0$n" else "$n"
