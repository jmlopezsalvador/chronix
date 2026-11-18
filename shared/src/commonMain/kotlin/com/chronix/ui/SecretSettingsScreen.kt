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
import com.chronix.domain.ForceMode
import com.chronix.domain.ForceTarget
import com.chronix.domain.StopSubmode
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
    onOpenMagicFriends: () -> Unit,
) {
    val focus = LocalFocusManager.current
    var warning by remember { mutableStateOf<String?>(null) }
    // Si la guía de inicio sigue activa, al entrar se recomienda desactivarla
    var guideAdvice by remember { mutableStateOf(settings.showGuide) }
    var pending by remember { mutableStateOf<PendingAppearance?>(null) }

    val theme = ClockThemes.byId(settings.themeId)
    val totalsVisible = settings.totalsVisible()

    // Cambia un ajuste. Si tras el cambio ya no se ven los totales (diseño sin totales u opción desactivada) y estaba
    // elegido forzar el total, no tiene sentido: se pasa a «Tiempo de vuelta» y se avisa con un modal.
    val change: (AppSettings) -> Unit = { raw ->
        val newTheme = ClockThemes.byId(raw.themeId)
        val visible = raw.showTotals && newTheme.showsTotal
        val forcedToLap = !visible && raw.forceTarget == ForceTarget.TOTAL
        val new = if (forcedToLap) raw.copy(forceTarget = ForceTarget.LAP) else raw
        onChange(new)
        if (forcedToLap) {
            warning = if (!newTheme.showsTotal) {
                "El diseño «${newTheme.name}» no muestra el tiempo total de las vueltas, solo el tiempo de vuelta, así que no se puede forzar el total. " +
                    "«Forzar las centésimas en» se ha cambiado a «Tiempo de vuelta»."
            } else {
                "Has ocultado los tiempos totales, así que no se pueden forzar. " +
                    "«Forzar las centésimas en» se ha cambiado a «Tiempo de vuelta»."
            }
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
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Mostrar tiempos totales", color = if (theme.showsTotal) Color.Black else Color.Gray)
                Text(
                    if (theme.showsTotal) "Si los ocultas, las vueltas solo enseñan su tiempo de vuelta."
                    else "El diseño ${theme.name} no tiene columna de totales.",
                    fontSize = 13.sp, color = Color.Gray,
                )
            }
            Switch(
                checked = totalsVisible, enabled = theme.showsTotal,
                onCheckedChange = { on -> focus.clearFocus(); change(settings.copy(showTotals = on)) },
            )
        }

        // --- Forzaje
        Section("Modo de forzaje")
        listOf(
            ForceMode.LAPS to "Por vueltas (en cada vuelta)",
            ForceMode.STOPWATCH to "Cronómetro (al pulsar Parar)",
        ).forEach { (mode, label) ->
            Row(Modifier.fillMaxWidth().clickable { focus.clearFocus(); change(settings.copy(forceMode = mode)) },
                verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = settings.forceMode == mode, onClick = { focus.clearFocus(); change(settings.copy(forceMode = mode)) })
                Text(label, color = Color.Black)
            }
        }

        if (settings.forceMode == ForceMode.LAPS) {
            Section("Forzar las centésimas en")
            Text(
                if (totalsVisible) "Lo normal es el tiempo de vuelta; el total sale como consecuencia de las vueltas."
                else "«Tiempo total» no está disponible porque los totales no se ven.",
                fontSize = 13.sp, color = Color.Gray,
            )
            listOf(
                ForceTarget.LAP to "Tiempo de vuelta (recomendado)",
                ForceTarget.TOTAL to "Tiempo total",
            ).forEach { (target, label) ->
                val enabled = target == ForceTarget.LAP || totalsVisible
                Row(
                    Modifier.fillMaxWidth().let { if (enabled) it.clickable { focus.clearFocus(); change(settings.copy(forceTarget = target)) } else it },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = settings.forceTarget == target, enabled = enabled,
                        onClick = { focus.clearFocus(); change(settings.copy(forceTarget = target)) },
                    )
                    Text(label, color = if (enabled) Color.Black else Color.Gray)
                }
            }

            Section("Centésimas forzadas por vuelta")
            Text("Con el forzaje armado (pulsación larga en Restablecer), la vuelta n termina en estas centésimas.",
                fontSize = 13.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            settings.forcedCs.forEachIndexed { i, v ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vuelta ${i + 1}", color = Color.Black, modifier = Modifier.width(90.dp))
                    NumberField(value = v, max = 99, width = 100.dp,
                        onValue = { n -> onChange(settings.copy(forcedCs = settings.forcedCs.toMutableList().also { it[i] = n })) })
                    TextButton(onClick = {
                        focus.clearFocus()
                        onChange(settings.copy(forcedCs = settings.forcedCs.toMutableList().also { it.removeAt(i) }))
                    }) { Text("Quitar") }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { focus.clearFocus(); onChange(settings.copy(forcedCs = settings.forcedCs + 0)) }) { Text("Añadir vuelta") }
        } else {
            Section("Qué forzar al parar")
            Text("Las vueltas no se tocan. El forzaje actúa una sola vez, la primera vez que pulsas Parar tras armarlo; " +
                "después puedes seguir parando e iniciando sin que se vuelva a forzar. La vuelta en curso se ajusta sola " +
                "para que cuadre con el contador.", fontSize = 13.sp, color = Color.Gray)
            listOf(
                StopSubmode.CENTIS to "Solo centésimas del contador grande",
                StopSubmode.FULL to "Contador completo (minutos, segundos y centésimas)",
            ).forEach { (sub, label) ->
                Row(Modifier.fillMaxWidth().clickable { focus.clearFocus(); onChange(settings.copy(stopSubmode = sub)) },
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = settings.stopSubmode == sub, onClick = { focus.clearFocus(); onChange(settings.copy(stopSubmode = sub)) })
                    Text(label, color = Color.Black)
                }
            }

            if (settings.stopSubmode == StopSubmode.CENTIS) {
                Section("Centésimas al parar")
                Text("Las centésimas del contador grande al parar. Los minutos y segundos no se tocan: se quedan en los que " +
                    "estás o pasan al siguiente, nunca retroceden.", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Centésimas", color = Color.Black, modifier = Modifier.width(110.dp))
                    NumberField(value = settings.stopCentis, max = 99, width = 100.dp,
                        onValue = { n -> onChange(settings.copy(stopCentis = n)) })
                }
            } else {
                Section("Contador al parar")
                Text("Minutos, segundos y centésimas con los que se detiene el contador grande. Se sustituyen tal cual, " +
                    "aunque el contador tenga que retroceder.", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                val v = settings.stopFullCs
                val m = (v / 6000).toInt()
                val sec = ((v / 100) % 60).toInt()
                val c = (v % 100).toInt()
                fun set(nm: Int, ns: Int, nc: Int) = onChange(settings.copy(stopFullCs = nm * 6000L + ns * 100L + nc))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    NumberField(value = m, max = 99, width = 76.dp, onValue = { set(it, sec, c) })
                    Text(":", fontSize = 20.sp, color = Color.Black)
                    NumberField(value = sec, max = 59, width = 76.dp, onValue = { set(m, it, c) })
                    Text(",", fontSize = 20.sp, color = Color.Black)
                    NumberField(value = c, max = 99, width = 76.dp, onValue = { set(m, sec, it) })
                }
            }
        }

        Section("Pulsación larga para abrir los ajustes (en una zona vacía)")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { focus.clearFocus(); onChange(settings.copy(secretHoldMs = (settings.secretHoldMs - 100).coerceAtLeast(500))) }) { Text("−") }
            Text(formatSeconds(settings.secretHoldMs), fontSize = 20.sp, color = Color.Black,
                modifier = Modifier.width(90.dp).padding(horizontal = 8.dp))
            OutlinedButton(onClick = { focus.clearFocus(); onChange(settings.copy(secretHoldMs = (settings.secretHoldMs + 100).coerceAtMost(5000))) }) { Text("+") }
        }

        // --- Magic Friends: un ayudante cambia en remoto los ajustes de forzaje. Solo con el interruptor activo se abre la red.
        val mfConfigured = MagicFriendsConfig.configured
        val mfOn = settings.magicFriendsEnabled && mfConfigured
        Section("Magic Friends")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Usar Magic Friends", color = if (mfConfigured) Color.Black else Color.Gray)
                Text(
                    if (mfConfigured) "Un ayudante puede cambiar en remoto los ajustes de forzaje. Solo con esto activado se usa la red."
                    else "Esta compilación no tiene configurado el servicio.",
                    fontSize = 13.sp, color = Color.Gray,
                )
            }
            Switch(
                checked = mfOn, enabled = mfConfigured,
                onCheckedChange = { on -> focus.clearFocus(); onChange(settings.copy(magicFriendsEnabled = on)) },
            )
        }
        if (mfOn) {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = { focus.clearFocus(); onOpenMagicFriends() }) { Text("Sesiones") }
        }

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
            title = { Text("Forzar en el total no disponible") },
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
 * Caja numérica de 0 a [max] (2 dígitos como máximo): se puede borrar y escribir con normalidad. El valor se guarda al
 * teclear (vacío cuenta como 0) y al salir de la caja se muestra con dos cifras.
 */
@Composable
private fun NumberField(value: Int, max: Int, width: Dp, onValue: (Int) -> Unit) {
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

@Composable
internal fun Section(title: String) {
    Spacer(Modifier.height(24.dp))
    Text(title, fontSize = 16.sp, color = Color(0xFF3F3FE0))
    Spacer(Modifier.height(6.dp))
}

private fun formatSeconds(ms: Long): String = "${ms / 1000},${(ms % 1000) / 100} s"
