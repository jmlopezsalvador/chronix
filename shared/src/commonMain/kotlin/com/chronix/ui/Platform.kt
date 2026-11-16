package com.chronix.ui

import androidx.compose.runtime.Composable

/**
 * Lo que depende del sistema (barras, sensores, vibración...) se declara aquí con `expect`
 * y se implementa en androidMain / iosMain. Todo lo demás va en común.
 */
@Composable
expect fun SystemBarsEffect(darkBackground: Boolean)

/** Devuelve una función que hace vibrar el móvil (pulso corto y firme). */
@Composable
expect fun rememberVibrate(): () -> Unit
