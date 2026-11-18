package com.chronix.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
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
    onOpenMagicFriends: () -> Unit,
    onOpenHelper: () -> Unit,
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

        // --- Forzaje (el mismo editor lo usa el ayudante de Magic Friends)
        ForcingEditor(
            values = settings.forcingValues(),
            totalsVisible = totalsVisible,
            onChange = { v ->
                val new = settings.withForcing(v)
                // El modo y «forzar en» pasan por `change` (avisa si el total ya no se ve); el resto se guarda directamente
                if (new.forceMode != settings.forceMode || new.forceTarget != settings.forceTarget) change(new) else onChange(new)
            },
        )

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
        if (MagicFriendsConfig.helperAvailable) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Controlar a otro mago: cambias en remoto sus ajustes de forzaje con el código que él te dé. La red solo se usa mientras esa pantalla está abierta.",
                fontSize = 13.sp, color = Color.Gray,
            )
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = { focus.clearFocus(); onOpenHelper() }) { Text("Soy el ayudante") }
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

@Composable
internal fun Section(title: String) {
    Spacer(Modifier.height(24.dp))
    Text(title, fontSize = 16.sp, color = Color(0xFF3F3FE0))
    Spacer(Modifier.height(6.dp))
}

private fun formatSeconds(ms: Long): String = "${ms / 1000},${(ms % 1000) / 100} s"
