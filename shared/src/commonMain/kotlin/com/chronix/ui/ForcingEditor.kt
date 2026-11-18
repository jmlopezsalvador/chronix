package com.chronix.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronix.domain.ForceMode
import com.chronix.domain.ForceTarget
import com.chronix.domain.StopSubmode

/**
 * Los valores del forzaje que se editan: los ajustes locales del mago y el plan completo que manda el ayudante de Magic
 * Friends. Son las mismas seis claves de la lista blanca remota ([RemoteKeys]); por defecto, los mismos valores que [AppSettings].
 */
data class ForcingValues(
    val forceMode: ForceMode = ForceMode.LAPS,
    val forceTarget: ForceTarget = ForceTarget.LAP,
    val forcedCs: List<Int> = listOf(0, 0, 0, 0),
    val stopSubmode: StopSubmode = StopSubmode.CENTIS,
    val stopCentis: Int = 0,
    val stopFullCs: Long = 0L,
)

fun AppSettings.forcingValues() = ForcingValues(forceMode, forceTarget, forcedCs, stopSubmode, stopCentis, stopFullCs)

fun AppSettings.withForcing(v: ForcingValues) = copy(
    forceMode = v.forceMode,
    forceTarget = v.forceTarget,
    forcedCs = v.forcedCs,
    stopSubmode = v.stopSubmode,
    stopCentis = v.stopCentis,
    stopFullCs = v.stopFullCs,
)

/**
 * Editor del forzaje: modo, «forzar en», lista de centésimas por vuelta y qué forzar al parar. Lo usan los ajustes secretos
 * (valores locales) y la pantalla del ayudante ([remote] = true: valores que se mandan al móvil del mago, con otros textos).
 * Emite el estado completo nuevo en [onChange]; quien lo usa decide cuándo guardarlo o enviarlo.
 */
@Composable
internal fun ForcingEditor(
    values: ForcingValues,
    onChange: (ForcingValues) -> Unit,
    totalsVisible: Boolean = true,
    remote: Boolean = false,
) {
    val focus = LocalFocusManager.current

    Section("Modo de forzaje")
    listOf(
        ForceMode.LAPS to "Por vueltas (en cada vuelta)",
        ForceMode.STOPWATCH to "Cronómetro (al pulsar Parar)",
    ).forEach { (mode, label) ->
        Row(Modifier.fillMaxWidth().clickable { focus.clearFocus(); onChange(values.copy(forceMode = mode)) },
            verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = values.forceMode == mode, onClick = { focus.clearFocus(); onChange(values.copy(forceMode = mode)) })
            Text(label, color = Color.Black)
        }
    }

    if (values.forceMode == ForceMode.LAPS) {
        Section("Forzar las centésimas en")
        Text(
            when {
                remote -> "Si el móvil del mago no muestra los tiempos totales, se forzará el tiempo de vuelta aunque elijas «Tiempo total»."
                totalsVisible -> "Lo normal es el tiempo de vuelta; el total sale como consecuencia de las vueltas."
                else -> "«Tiempo total» no está disponible porque los totales no se ven."
            },
            fontSize = 13.sp, color = Color.Gray,
        )
        listOf(
            ForceTarget.LAP to "Tiempo de vuelta (recomendado)",
            ForceTarget.TOTAL to "Tiempo total",
        ).forEach { (target, label) ->
            val enabled = target == ForceTarget.LAP || totalsVisible
            Row(
                Modifier.fillMaxWidth().let { if (enabled) it.clickable { focus.clearFocus(); onChange(values.copy(forceTarget = target)) } else it },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = values.forceTarget == target, enabled = enabled,
                    onClick = { focus.clearFocus(); onChange(values.copy(forceTarget = target)) },
                )
                Text(label, color = if (enabled) Color.Black else Color.Gray)
            }
        }

        Section("Centésimas forzadas por vuelta")
        Text(
            if (remote) "Con el forzaje armado en el móvil del mago, la vuelta n termina en estas centésimas."
            else "Con el forzaje armado (pulsación larga en Restablecer), la vuelta n termina en estas centésimas.",
            fontSize = 13.sp, color = Color.Gray,
        )
        Spacer(Modifier.height(8.dp))
        values.forcedCs.forEachIndexed { i, v ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Vuelta ${i + 1}", color = Color.Black, modifier = Modifier.width(90.dp))
                NumberField(value = v, max = 99, width = 100.dp,
                    onValue = { n -> onChange(values.copy(forcedCs = values.forcedCs.toMutableList().also { it[i] = n })) })
                TextButton(onClick = {
                    focus.clearFocus()
                    onChange(values.copy(forcedCs = values.forcedCs.toMutableList().also { it.removeAt(i) }))
                }) { Text("Quitar") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { focus.clearFocus(); onChange(values.copy(forcedCs = values.forcedCs + 0)) }) { Text("Añadir vuelta") }
    } else {
        Section("Qué forzar al parar")
        Text(
            if (remote) {
                "Las vueltas no se tocan. El forzaje actúa una sola vez, la primera vez que el mago pulsa Parar tras armarlo; " +
                    "después puede seguir parando e iniciando sin que se vuelva a forzar. La vuelta en curso se ajusta sola " +
                    "para que cuadre con el contador."
            } else {
                "Las vueltas no se tocan. El forzaje actúa una sola vez, la primera vez que pulsas Parar tras armarlo; " +
                    "después puedes seguir parando e iniciando sin que se vuelva a forzar. La vuelta en curso se ajusta sola " +
                    "para que cuadre con el contador."
            },
            fontSize = 13.sp, color = Color.Gray,
        )
        listOf(
            StopSubmode.CENTIS to "Solo centésimas del contador grande",
            StopSubmode.FULL to "Contador completo (minutos, segundos y centésimas)",
        ).forEach { (sub, label) ->
            Row(Modifier.fillMaxWidth().clickable { focus.clearFocus(); onChange(values.copy(stopSubmode = sub)) },
                verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = values.stopSubmode == sub, onClick = { focus.clearFocus(); onChange(values.copy(stopSubmode = sub)) })
                Text(label, color = Color.Black)
            }
        }

        if (values.stopSubmode == StopSubmode.CENTIS) {
            Section("Centésimas al parar")
            Text("Las centésimas del contador grande al parar. Los minutos y segundos no se tocan: se quedan en los que " +
                "estás o pasan al siguiente, nunca retroceden.", fontSize = 13.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Centésimas", color = Color.Black, modifier = Modifier.width(110.dp))
                NumberField(value = values.stopCentis, max = 99, width = 100.dp,
                    onValue = { n -> onChange(values.copy(stopCentis = n)) })
            }
        } else {
            Section("Contador al parar")
            Text("Minutos, segundos y centésimas con los que se detiene el contador grande. Se sustituyen tal cual, " +
                "aunque el contador tenga que retroceder.", fontSize = 13.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            val v = values.stopFullCs
            val m = (v / 6000).toInt()
            val sec = ((v / 100) % 60).toInt()
            val c = (v % 100).toInt()
            fun set(nm: Int, ns: Int, nc: Int) = onChange(values.copy(stopFullCs = nm * 6000L + ns * 100L + nc))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                NumberField(value = m, max = 99, width = 76.dp, onValue = { set(it, sec, c) })
                Text(":", fontSize = 20.sp, color = Color.Black)
                NumberField(value = sec, max = 59, width = 76.dp, onValue = { set(m, it, c) })
                Text(",", fontSize = 20.sp, color = Color.Black)
                NumberField(value = c, max = 99, width = 76.dp, onValue = { set(m, sec, it) })
            }
        }
    }
}

/**
 * Caja numérica de 0 a [max] (2 dígitos como máximo): se puede borrar y escribir con normalidad. El valor se guarda al
 * teclear (vacío cuenta como 0) y al salir de la caja se muestra con dos cifras.
 */
@Composable
internal fun NumberField(value: Int, max: Int, width: Dp, onValue: (Int) -> Unit) {
    val focus = LocalFocusManager.current
    var text by remember { mutableStateOf(pad2(value)) }
    var focused by remember { mutableStateOf(false) }
    // Si el valor cambia desde fuera (p. ej. se quita otra fila), se refleja
    LaunchedEffect(value) { if ((text.toIntOrNull() ?: 0) != value) text = pad2(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.take(2)
            val n = (digits.toIntOrNull() ?: 0).coerceAtMost(max)
            // Si se pasa del máximo (p. ej. 75 segundos) se queda en el máximo
            text = if ((digits.toIntOrNull() ?: 0) > max) pad2(n) else digits
            onValue(n)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        modifier = Modifier.width(width).onFocusChanged {
            if (focused && !it.isFocused) text = pad2(text.toIntOrNull() ?: 0)
            focused = it.isFocused
        },
    )
}
