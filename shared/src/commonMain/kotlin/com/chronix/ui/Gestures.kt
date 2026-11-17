package com.chronix.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Pulsación corta → [onTap] al soltar. Si se mantiene [holdMs] → [onHold] (una sola vez, sin disparar el tap al soltar).
 */
@Composable
fun Modifier.pressAndHold(holdMs: Long, onHold: () -> Unit, onTap: () -> Unit = {}): Modifier {
    val hold by rememberUpdatedState(onHold)
    val tap by rememberUpdatedState(onTap)
    return pointerInput(holdMs) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var finished = false
            val up = withTimeoutOrNull(holdMs) {
                val u = waitForUpOrCancellation()
                finished = true
                u
            }
            if (!finished) {
                hold()
                waitForUpOrCancellation()
            } else if (up != null) {
                tap()
            }
        }
    }
}

/** Estado compartido entre el detector de la raíz y los controles que lo bloquean (ver [blocksSecret]). */
class SecretHoldState {
    /** True si el dedo que está pulsando empezó sobre un control (botón, barra de pestañas...). */
    var blocked = false
}

val LocalSecretHold = androidx.compose.runtime.compositionLocalOf<SecretHoldState?> { null }

/**
 * Acceso secreto: pulsación larga de [holdMs] en cualquier parte de la pantalla que **no sea un control**.
 * Se observa en la fase Initial sin consumir nada, así que no estorba a botones, listas ni al pager. Si el dedo se
 * mueve más del umbral o se pulsa con dos dedos, se cancela (un deslizamiento no cuenta). Va en el nodo raíz,
 * dentro de un `CompositionLocalProvider(LocalSecretHold provides state)`.
 */
@Composable
fun Modifier.secretAccess(state: SecretHoldState, holdMs: Long, onHold: () -> Unit): Modifier {
    val hold by rememberUpdatedState(onHold)
    return pointerInput(state, holdMs) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            // Los controles marcan `blocked` durante este mismo evento; se limpia al terminar el gesto (más abajo)
            val slop = viewConfiguration.touchSlop
            var cancelled = false
            withTimeoutOrNull(holdMs) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null || !change.pressed || event.changes.size > 1 ||
                        (change.position - down.position).getDistance() > slop
                    ) {
                        cancelled = true
                        break
                    }
                }
            }
            if (!cancelled && !state.blocked) hold()
            // Espera a que se levante el dedo antes de empezar otro gesto
            if (!cancelled) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.none { it.pressed }) break
                }
            }
            state.blocked = false
        }
    }
}

/**
 * Marca un control (botón, pestañas...) para que una pulsación larga sobre él **no** abra los ajustes. No consume
 * nada: los clics y gestos del propio control siguen funcionando.
 */
@Composable
fun Modifier.blocksSecret(): Modifier {
    val state = LocalSecretHold.current ?: return this
    return pointerInput(state) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            state.blocked = true
        }
    }
}
