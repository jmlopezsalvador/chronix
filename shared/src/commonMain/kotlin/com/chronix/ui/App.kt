package com.chronix.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.magicfriends.client.ClientEvent

private enum class Screen { STOPWATCH, SETTINGS, INSTRUCTIONS, MAGIC_FRIENDS }

/**
 * Raíz Compose común (Android e iOS). Muestra el cronómetro con el look and feel elegido y, con una pulsación larga
 * en una zona vacía, los ajustes. El cronómetro se conserva al entrar y salir de los ajustes. Al abrir la app sale la
 * guía de inicio si está activa.
 *
 * **Magic Friends:** con el interruptor de los ajustes activado (y la compilación configurada) se crea el
 * [MagicFriendsController], que abre la red y recibe los ajustes remotos; con él apagado no hay controlador ni red.
 */
@Composable
fun ChronixRoot(store: SettingsStore) {
    var settings by remember { mutableStateOf(store.load()) }
    var screen by remember { mutableStateOf(Screen.STOPWATCH) }
    var guideOpen by remember { mutableStateOf(settings.showGuide) }
    val stopwatch = remember { StopwatchState() }
    val vibrate = rememberVibrate()
    val softVibrate = rememberSoftVibrate()
    val appearance = rememberAppAppearance()
    val secretState = remember { SecretHoldState() }
    val scope = rememberCoroutineScope()
    val theme = ClockThemes.byId(settings.themeId)
    SystemBarsEffect(darkBackground = theme.dark && screen == Screen.STOPWATCH)

    val update: (AppSettings) -> Unit = { settings = it; store.save(it) }

    // Magic Friends: el controlador solo existe con el interruptor activado y la compilación configurada.
    var magic by remember { mutableStateOf<MagicFriendsController?>(null) }
    DisposableEffect(settings.magicFriendsEnabled) {
        val controller = if (settings.magicFriendsEnabled && MagicFriendsConfig.configured) {
            MagicFriendsController(store.prefs, scope).also { it.start() }
        } else null
        magic = controller
        onDispose {
            controller?.stop()
            magic = null
        }
    }
    // Aviso discreto (vibración suave) cuando llega un cambio del ayudante
    LaunchedEffect(magic) {
        magic?.client?.events?.collect { if (it is ClientEvent.RemotePatch) softVibrate() }
    }

    MaterialTheme {
        when (screen) {
            Screen.STOPWATCH -> CompositionLocalProvider(LocalSecretHold provides secretState) {
                // Pulsación larga en cualquier zona vacía → ajustes (los controles la bloquean, ver blocksSecret)
                Box(Modifier.fillMaxSize().secretAccess(secretState, settings.secretHoldMs) { screen = Screen.SETTINGS }) {
                    StopwatchScreen(
                        state = stopwatch,
                        theme = theme,
                        showTotals = settings.totalsVisible(),
                        onArmForcing = {
                            // Plan vivo: se lee el ajuste efectivo (local + remoto) en cada vuelta y en cada Parar,
                            // no solo al armar, para que el ayudante pueda cambiarlo hasta el último instante.
                            stopwatch.resetAndArm { settings.forcePlan(magic?.remoteOverrides()) }
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
                onOpenMagicFriends = { screen = Screen.MAGIC_FRIENDS },
            )
            Screen.INSTRUCTIONS -> InstructionsScreen(onBack = { screen = Screen.SETTINGS })
            Screen.MAGIC_FRIENDS -> MagicFriendsScreen(controller = magic, onBack = { screen = Screen.SETTINGS })
        }

        if (guideOpen) {
            GuideDialog(
                onClose = { guideOpen = false },
                onNeverShow = { guideOpen = false; update(settings.copy(showGuide = false)) },
            )
        }
    }
}
