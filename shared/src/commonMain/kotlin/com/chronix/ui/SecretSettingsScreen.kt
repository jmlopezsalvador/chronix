package com.chronix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Ajustes secretos: look and feel, duración de la pulsación de acceso y centésimas forzadas por vuelta. */
@Composable
fun SecretSettingsScreen(settings: AppSettings, onChange: (AppSettings) -> Unit, onBack: () -> Unit) {
    val focus = LocalFocusManager.current
    Column(
        Modifier.fillMaxSize().background(Color.White).windowInsetsPadding(WindowInsets.safeDrawing)
            .pointerInput(Unit) { detectTapGestures { focus.clearFocus() } }
            .verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { focus.clearFocus(); onBack() }) { Text("‹ Volver", fontSize = 18.sp) }
            Spacer(Modifier.weight(1f))
        }
        Text("Ajustes", fontSize = 28.sp, color = Color.Black)

        Section("Look and feel")
        ClockThemes.all.forEach { t ->
            Row(Modifier.fillMaxWidth().clickable { focus.clearFocus(); onChange(settings.copy(themeId = t.id)) },
                verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = settings.themeId == t.id, onClick = { focus.clearFocus(); onChange(settings.copy(themeId = t.id)) })
                Text(t.name, color = Color.Black)
            }
        }

        Section("Pulsación larga de acceso (esquina superior derecha)")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { focus.clearFocus(); onChange(settings.copy(secretHoldMs = (settings.secretHoldMs - 100).coerceAtLeast(500))) }) { Text("−") }
            Text(formatSeconds(settings.secretHoldMs), fontSize = 20.sp, color = Color.Black,
                modifier = Modifier.width(90.dp).padding(horizontal = 8.dp))
            OutlinedButton(onClick = { focus.clearFocus(); onChange(settings.copy(secretHoldMs = (settings.secretHoldMs + 100).coerceAtMost(5000))) }) { Text("+") }
        }

        Section("Centésimas forzadas por vuelta")
        Text("Con el forzaje armado (pulsación larga en Restablecer), la vuelta n termina en estas centésimas.",
            fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        settings.forcedCs.forEachIndexed { i, v ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Vuelta ${i + 1}", color = Color.Black, modifier = Modifier.width(90.dp))
                CentisField(
                    value = v,
                    onValue = { n -> onChange(settings.copy(forcedCs = settings.forcedCs.toMutableList().also { it[i] = n })) },
                )
                TextButton(onClick = {
                    focus.clearFocus()
                    onChange(settings.copy(forcedCs = settings.forcedCs.toMutableList().also { it.removeAt(i) }))
                }) { Text("Quitar") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { focus.clearFocus(); onChange(settings.copy(forcedCs = settings.forcedCs + 0)) }) { Text("Añadir vuelta") }
        Spacer(Modifier.height(40.dp))
    }
}

/**
 * Caja numérica de 0 a 99: se puede borrar y escribir con normalidad (máx. 2 dígitos). El valor se guarda al
 * teclear (vacío cuenta como 0) y al salir de la caja se muestra con dos cifras.
 */
@Composable
private fun CentisField(value: Int, onValue: (Int) -> Unit) {
    val focus = LocalFocusManager.current
    var text by remember { mutableStateOf(pad2(value)) }
    var focused by remember { mutableStateOf(false) }
    // Si el valor cambia desde fuera (p. ej. se quita otra fila), se refleja
    LaunchedEffect(value) { if ((text.toIntOrNull() ?: 0) != value) text = pad2(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.take(2)
            text = digits
            onValue(digits.toIntOrNull() ?: 0)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        modifier = Modifier.width(100.dp).onFocusChanged {
            if (focused && !it.isFocused) text = pad2(text.toIntOrNull() ?: 0)
            focused = it.isFocused
        },
    )
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(24.dp))
    Text(title, fontSize = 16.sp, color = Color(0xFF3F3FE0))
    Spacer(Modifier.height(6.dp))
}

private fun formatSeconds(ms: Long): String = "${ms / 1000},${(ms % 1000) / 100} s"
