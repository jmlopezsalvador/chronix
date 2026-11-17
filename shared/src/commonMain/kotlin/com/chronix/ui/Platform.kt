package com.chronix.ui

import androidx.compose.runtime.Composable

/**
 * Lo que depende del sistema (barras, sensores, vibración...) se declara aquí con `expect`
 * y se implementa en androidMain / iosMain. Todo lo demás va en común.
 */
@Composable
expect fun SystemBarsEffect(darkBackground: Boolean)

/**
 * Cambio de icono y de nombre de la app (lo que ve el usuario en el lanzador). Android lo hace con un `activity-alias`
 * por combinación y reinicia la app; iOS solo puede cambiar el icono (con un aviso del sistema, sin reiniciar).
 */
interface AppAppearance {
    /** Si la plataforma permite cambiar el nombre (iOS no). */
    val canChangeName: Boolean

    /** Si aplicar el cambio reinicia la app (Android sí; iOS enseña un aviso del sistema). */
    val restartsApp: Boolean

    /** Aplica el icono y el nombre (ids de [AppIconCatalog]). En Android reinicia la app al terminar. */
    fun apply(iconId: String, nameId: String)
}

@Composable
expect fun rememberAppAppearance(): AppAppearance

/** Devuelve una función que hace vibrar el móvil (pulso corto y firme). */
@Composable
expect fun rememberVibrate(): () -> Unit
