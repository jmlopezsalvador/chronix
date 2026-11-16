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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Ajustes secretos: look and feel, duración de la pulsación de acceso y centésimas forzadas por vuelta. */
@Composable
fun SecretSettingsScreen(settings: AppSettings, onChange: (AppSettings) -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color.White).windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Volver", fontSize = 18.sp) }
            Spacer(Modifier.weight(1f))
        }
        Text("Ajustes", fontSize = 28.sp, color = Color.Black)

        Section("Look and feel")
        ClockThemes.all.forEach { t ->
            Row(Modifier.fillMaxWidth().clickable { onChange(settings.copy(themeId = t.id)) },
                verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = settings.themeId == t.id, onClick = { onChange(settings.copy(themeId = t.id)) })
                Text(t.name, color = Color.Black)
            }
        }

        Section("Pulsación larga de acceso (esquina superior derecha)")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { onChange(settings.copy(secretHoldMs = (settings.secretHoldMs - 100).coerceAtLeast(500))) }) { Text("−") }
            Text(formatSeconds(settings.secretHoldMs), fontSize = 20.sp, color = Color.Black,
                modifier = Modifier.width(90.dp).padding(horizontal = 8.dp))
            OutlinedButton(onClick = { onChange(settings.copy(secretHoldMs = (settings.secretHoldMs + 100).coerceAtMost(5000))) }) { Text("+") }
        }

        Section("Centésimas forzadas por vuelta")
        Text("Con el forzaje armado (pulsación larga en Restablecer), la vuelta n termina en estas centésimas.",
            fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        settings.forcedCs.forEachIndexed { i, v ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Vuelta ${i + 1}", color = Color.Black, modifier = Modifier.width(90.dp))
                OutlinedTextField(
                    value = if (v < 10) "0$v" else "$v",
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.takeLast(2)
                        val n = digits.toIntOrNull() ?: 0
                        onChange(settings.copy(forcedCs = settings.forcedCs.toMutableList().also { it[i] = n }))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(100.dp),
                )
                TextButton(onClick = {
                    onChange(settings.copy(forcedCs = settings.forcedCs.toMutableList().also { it.removeAt(i) }))
                }) { Text("Quitar") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onChange(settings.copy(forcedCs = settings.forcedCs + 0)) }) { Text("Añadir vuelta") }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(24.dp))
    Text(title, fontSize = 16.sp, color = Color(0xFF3F3FE0))
    Spacer(Modifier.height(6.dp))
}

private fun formatSeconds(ms: Long): String = "${ms / 1000},${(ms % 1000) / 100} s"
