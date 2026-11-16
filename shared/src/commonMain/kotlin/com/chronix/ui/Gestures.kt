package com.chronix.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
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
