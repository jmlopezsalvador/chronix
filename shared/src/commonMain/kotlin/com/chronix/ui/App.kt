package com.chronix.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Raíz Compose común (Android e iOS). Muestra el cronómetro con el look and feel elegido y,
 * con la pulsación larga secreta, los ajustes. El cronómetro se conserva al entrar y salir de los ajustes.
 */
@Composable
fun ChronixRoot(store: SettingsStore) {
    var settings by remember { mutableStateOf(store.load()) }
    var inSettings by remember { mutableStateOf(false) }
    val stopwatch = remember { StopwatchState() }
    val vibrate = rememberVibrate()
    val theme = ClockThemes.byId(settings.themeId)
    SystemBarsEffect(darkBackground = theme.dark && !inSettings)

    MaterialTheme {
        if (inSettings) {
            SecretSettingsScreen(
                settings = settings,
                onChange = { settings = it; store.save(it) },
                onBack = { inSettings = false },
            )
        } else {
            StopwatchScreen(
                state = stopwatch,
                theme = theme,
                secretHoldMs = settings.secretHoldMs,
                onSecret = { inSettings = true },
                onArmForcing = {
                    stopwatch.resetAndArm(settings.forcedCs)
                    vibrate()
                },
            )
        }
    }
}
