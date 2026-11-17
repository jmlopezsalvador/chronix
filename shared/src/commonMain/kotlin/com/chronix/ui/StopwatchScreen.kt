package com.chronix.ui

import androidx.compose.runtime.Composable

/**
 * Pantalla del cronómetro: elige el diseño según el look and feel. Todos son una réplica del reloj real
 * y ninguno muestra nada del forzaje. El acceso a los ajustes (pulsación larga en zona vacía) lo gestiona la raíz.
 */
@Composable
fun StopwatchScreen(
    state: StopwatchState,
    theme: ClockTheme,
    showTotals: Boolean,
    onArmForcing: () -> Unit,
) {
    val p = StopwatchScreenParams(state, theme, showTotals, onArmForcing)
    TickEffect(state)
    when (theme.style) {
        ClockStyle.SAMSUNG -> SamsungStopwatch(p)
        ClockStyle.IPHONE -> IPhoneStopwatch(p)
        ClockStyle.PIXEL -> PixelStopwatch(p)
        ClockStyle.ONEPLUS -> OnePlusStopwatch(p)
        ClockStyle.OPPO -> OppoStopwatch(p)
        ClockStyle.XIAOMI -> XiaomiStopwatch(p)
        ClockStyle.HUAWEI -> HuaweiStopwatch(p)
    }
}
