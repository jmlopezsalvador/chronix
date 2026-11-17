package com.chronix.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

private enum class Screen { STOPWATCH, SETTINGS, INSTRUCTIONS }

/**
 * Raíz Compose común (Android e iOS). Muestra el cronómetro con el look and feel elegido y, con una pulsación larga
 * en una zona vacía, los ajustes. El cronómetro se conserva al entrar y salir de los ajustes. Al abrir la app sale la
 * guía de inicio si está activa.
 */
@Composable
fun ChronixRoot(store: SettingsStore) {
    var settings by remember { mutableStateOf(store.load()) }
    var screen by remember { mutableStateOf(Screen.STOPWATCH) }
    var guideOpen by remember { mutableStateOf(settings.showGuide) }
    val stopwatch = remember { StopwatchState() }
    val vibrate = rememberVibrate()
    val appearance = rememberAppAppearance()
    val secretState = remember { SecretHoldState() }
    val theme = ClockThemes.byId(settings.themeId)
    SystemBarsEffect(darkBackground = theme.dark && screen == Screen.STOPWATCH)

    val update: (AppSettings) -> Unit = { settings = it; store.save(it) }

    MaterialTheme {
        when (screen) {
            Screen.STOPWATCH -> CompositionLocalProvider(LocalSecretHold provides secretState) {
                // Pulsación larga en cualquier zona vacía → ajustes (los controles la bloquean, ver blocksSecret)
                Box(Modifier.fillMaxSize().secretAccess(secretState, settings.secretHoldMs) { screen = Screen.SETTINGS }) {
                    StopwatchScreen(
                        state = stopwatch,
                        theme = theme,
                        onArmForcing = {
                            stopwatch.resetAndArm(settings.forcedCs, settings.forceTarget)
                            vibrate()
                        },
                    )
                }
            }
            Screen.SETTINGS -> SecretSettingsScreen(
                settings = settings,
                onChange = update,
                onBack = { screen = Screen.STOPWATCH },
                appearance = appearance,
                onShowGuide = { guideOpen = true },
                onOpenInstructions = { screen = Screen.INSTRUCTIONS },
            )
            Screen.INSTRUCTIONS -> InstructionsScreen(onBack = { screen = Screen.SETTINGS })
        }

        if (guideOpen) {
            GuideDialog(
                onClose = { guideOpen = false },
                onNeverShow = { guideOpen = false; update(settings.copy(showGuide = false)) },
            )
        }
    }
}
