package com.chronix.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronix.domain.ForceTarget
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

/** Cambio de icono o de nombre pendiente de que el usuario lo confirme en el modal. */
private class PendingAppearance(val iconId: String, val nameId: String, val what: String)

/**
 * Ajustes secretos: look and feel, forzaje, acceso, icono y nombre de la app, guía de inicio e instrucciones.
 * Cambiar el icono o el nombre pide confirmación (reinicia la app en Android).
 */
@Composable
fun SecretSettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onBack: () -> Unit,
    appearance: AppAppearance,
    onShowGuide: () -> Unit,
    onOpenInstructions: () -> Unit,
) {
    val focus = LocalFocusManager.current
    var warning by remember { mutableStateOf<String?>(null) }
    // Si la guía de inicio sigue activa, al entrar se recomienda desactivarla
    var guideAdvice by remember { mutableStateOf(settings.showGuide) }
    var pending by remember { mutableStateOf<PendingAppearance?>(null) }

    // Cambia un ajuste; si queda «forzar en total» con un diseño que no muestra totales, avisa con un modal
    val change: (AppSettings) -> Unit = { new ->
        onChange(new)
        val theme = ClockThemes.byId(new.themeId)
        val touched = new.themeId != settings.themeId || new.forceTarget != settings.forceTarget
        if (touched && new.forceTarget == ForceTarget.TOTAL && !theme.showsTotal) {
            warning = "El diseño «${theme.name}» no muestra el tiempo total de las vueltas, solo el tiempo de vuelta. " +
                "Con el forzaje en «Tiempo total» las centésimas forzadas no se verán en este diseño. " +
                "Elige «Tiempo de vuelta» u otro diseño."
        }
    }

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
            Row(Modifier.fillMaxWidth().clickable { focus.clearFocus(); change(settings.copy(themeId = t.id)) },
                verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = settings.themeId == t.id, onClick = { focus.clearFocus(); change(settings.copy(themeId = t.id)) })
                Text(t.name, color = Color.Black)
            }
        }

        Section("Forzar las centésimas en")
        Text("Lo normal es el tiempo de vuelta; el total sale como consecuencia de las vueltas.", fontSize = 13.sp, color = Color.Gray)
        listOf(
            ForceTarget.LAP to "Tiempo de vuelta (recomendado)",
            ForceTarget.TOTAL to "Tiempo total",
        ).forEach { (target, label) ->
            Row(Modifier.fillMaxWidth().clickable { focus.clearFocus(); change(settings.copy(forceTarget = target)) },
                verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = settings.forceTarget == target, onClick = { focus.clearFocus(); change(settings.copy(forceTarget = target)) })
                Text(label, color = Color.Black)
            }
        }

        Section("Pulsación larga para abrir los ajustes (en una zona vacía)")
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

        // --- Icono de la aplicación (desplegable)
        Spacer(Modifier.height(8.dp))
        Expandable("Icono de la aplicación") {
            AppIconCatalog.icons.forEach { icon ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        focus.clearFocus()
                        if (icon.id != settings.iconId) pending = PendingAppearance(icon.id, settings.nameId, "el icono")
                    }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIconImage(icon, 48.dp)
                    Text(icon.label, color = Color.Black, modifier = Modifier.weight(1f).padding(horizontal = 14.dp))
                    RadioButton(selected = icon.id == settings.iconId, onClick = {
                        focus.clearFocus()
                        if (icon.id != settings.iconId) pending = PendingAppearance(icon.id, settings.nameId, "el icono")
                    })
                }
            }
        }

        // --- Nombre de la aplicación (desplegable; iOS no permite cambiarlo)
        if (appearance.canChangeName) {
            Expandable("Nombre de la aplicación") {
                AppIconCatalog.names.forEach { name ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            focus.clearFocus()
                            if (name.id != settings.nameId) pending = PendingAppearance(settings.iconId, name.id, "el nombre")
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = name.id == settings.nameId, onClick = {
                            focus.clearFocus()
                            if (name.id != settings.nameId) pending = PendingAppearance(settings.iconId, name.id, "el nombre")
                        })
                        Text(name.label, color = Color.Black)
                    }
                }
            }
        }

        // --- Guía de inicio
        Section("Guía de inicio")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ocultar guía de inicio", color = Color.Black, modifier = Modifier.weight(1f))
            Switch(checked = !settings.showGuide, onCheckedChange = { hide -> focus.clearFocus(); onChange(settings.copy(showGuide = !hide)) })
        }
        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = { focus.clearFocus(); onShowGuide() }) { Text("Ver guía") }

        Section("Instrucciones")
        Button(onClick = { focus.clearFocus(); onOpenInstructions() }) { Text("Instrucciones") }

        Spacer(Modifier.height(40.dp))
        Text(CREDITS, fontSize = 13.sp, color = Color(0xFF7A7A80), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
    }

    // --- Modales
    warning?.let { msg ->
        AlertDialog(
            onDismissRequest = { warning = null },
            title = { Text("Este diseño no muestra el total") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { warning = null }) { Text("Entendido") } },
        )
    }
    if (guideAdvice) {
        AlertDialog(
            onDismissRequest = { guideAdvice = false },
            title = { Text("La guía de inicio está activa") },
            text = { Text("Cada vez que abras la app se mostrará la guía de inicio. Ya conoces cómo funciona, así que te recomendamos desactivarla.") },
            confirmButton = {
                TextButton(onClick = { guideAdvice = false; onChange(settings.copy(showGuide = false)) }) { Text("Desactivar guía") }
            },
            dismissButton = { TextButton(onClick = { guideAdvice = false }) { Text("Ahora no") } },
        )
    }
    pending?.let { p ->
        val restarts = appearance.restartsApp
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(if (p.what == "el icono") "¿Quieres cambiar el icono?" else "¿Quieres cambiar el nombre?") },
            text = {
                Text(
                    if (restarts) "Para cambiar ${p.what} de la aplicación hay que reiniciarla. Se cerrará y se volverá a abrir ahora mismo."
                    else "El sistema mostrará un aviso para confirmar el cambio de ${p.what} de la aplicación.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    // Primero se guarda el ajuste y después se aplica (en Android esto reinicia la app)
                    onChange(settings.copy(iconId = p.iconId, nameId = p.nameId))
                    appearance.apply(p.iconId, p.nameId)
                }) { Text("Aplicar") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancelar") } },
        )
    }
}

/** Miniatura de un icono de la app (PNG de 96 px incluido en base64 en el catálogo). */
@OptIn(ExperimentalEncodingApi::class, ExperimentalResourceApi::class)
@Composable
private fun AppIconImage(icon: AppIcon, size: Dp) {
    val bitmap = remember(icon.id) { Base64.decode(icon.thumbB64).decodeToImageBitmap() }
    Image(bitmap, contentDescription = icon.label, modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.22f)))
}

/** Apartado desplegable: se toca el título para abrirlo o cerrarlo. */
@Composable
private fun Expandable(title: String, content: @Composable ColumnScope.() -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { open = !open }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, fontSize = 16.sp, color = Color(0xFF3F3FE0), modifier = Modifier.weight(1f))
            Text(if (open) "▾" else "▸", fontSize = 18.sp, color = Color(0xFF3F3FE0))
        }
        if (open) content()
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
