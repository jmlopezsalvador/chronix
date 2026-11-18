package com.chronix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magicfriends.client.ConnectionState
import com.magicfriends.client.MfException
import com.magicfriends.client.PairingCode
import com.magicfriends.client.Session
import com.magicfriends.client.SessionStatus
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Caducidades que se pueden dar a una sesión nueva. */
private enum class ExpiryPreset(val label: String, val ms: Long?) {
    NONE("Sin caducidad", null),
    HOUR("1 hora", 3_600_000L),
    DAY("24 horas", 86_400_000L),
    WEEK("7 días", 604_800_000L),
}

/** Acción destructiva pendiente de confirmar. */
private class Confirm(val session: Session, val delete: Boolean)

/**
 * Pantalla secreta del mago (se abre desde los ajustes con Magic Friends activado): crear sesiones con nombre y
 * caducidad opcional, enseñar el código de emparejamiento, aceptar o rechazar a quien quiere unirse y activar, pausar,
 * revocar o borrar. Es un fondo blanco como el resto de los ajustes; el estado de la conexión solo se ve aquí.
 */
@Composable
internal fun MagicFriendsScreen(controller: MagicFriendsController?, onBack: () -> Unit) {
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
        Text("Magic Friends", fontSize = 28.sp, color = Color.Black)
        if (controller == null) {
            Spacer(Modifier.height(16.dp))
            Text("Magic Friends está desactivado. Actívalo en los ajustes para gestionar las sesiones.", color = Color.Gray)
        } else {
            MagicFriendsContent(controller)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun MagicFriendsContent(controller: MagicFriendsController) {
    val client = controller.client
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val connection by client.connection.collectAsState()
    val sessions by client.sessions.collectAsState()
    val pending by client.pending.collectAsState()

    var code by remember { mutableStateOf<PairingCode?>(null) }
    var codeFor by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    var name by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf(ExpiryPreset.NONE) }
    val connected = connection is ConnectionState.Connected

    /** Lanza una orden al servidor: si falla, enseña el motivo; si va bien, borra el aviso y ejecuta [onOk]. */
    fun <T> command(block: suspend () -> Result<T>, onOk: (T) -> Unit = {}) {
        scope.launch {
            block().onSuccess { message = null; onOk(it) }.onFailure { message = describeError(it) }
        }
    }

    Spacer(Modifier.height(8.dp))
    Text(
        when (connection) {
            ConnectionState.Connected -> "Conectado"
            ConnectionState.Connecting -> "Conectando…"
            ConnectionState.Disconnected -> "Sin conexión (se reintenta solo)"
            ConnectionState.AuthFailed -> "El servidor ha rechazado este dispositivo"
            ConnectionState.Idle -> "Inactivo"
        },
        fontSize = 14.sp, color = if (connected) Color(0xFF1B8A3A) else Color.Gray,
    )
    message?.let { Text(it, fontSize = 14.sp, color = Color(0xFFC62828), modifier = Modifier.padding(top = 4.dp)) }

    // --- Nueva sesión
    Section("Nueva sesión")
    OutlinedTextField(
        value = name, onValueChange = { name = it.take(40) }, singleLine = true,
        label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
    )
    Spacer(Modifier.height(8.dp))
    ExpiryPreset.entries.forEach { preset ->
        Row(
            Modifier.fillMaxWidth().clickable { focus.clearFocus(); expiry = preset },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = expiry == preset, onClick = { focus.clearFocus(); expiry = preset })
            Text(preset.label, color = Color.Black)
        }
    }
    Spacer(Modifier.height(8.dp))
    Button(
        enabled = connected && name.isNotBlank(),
        onClick = {
            focus.clearFocus()
            val sessionName = name.trim()
            val expiresAt = expiry.ms?.let { nowMs() + it }
            command({ client.createSession(sessionName, expiresAt) }) { created ->
                code = created.pairing
                codeFor = created.session.name
                name = ""
                expiry = ExpiryPreset.NONE
            }
        },
    ) { Text("Crear sesión") }
    if (!connected) {
        Text("Hace falta conexión para crear sesiones.", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
    }

    // --- Código de emparejamiento
    code?.let { c ->
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF1F1F6)).padding(16.dp)) {
            Text("Código para «$codeFor»", fontSize = 14.sp, color = Color.Gray)
            Text(c.code, fontSize = 36.sp, letterSpacing = 6.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
            Text("Un solo uso. Válido hasta las ${formatTime(c.expiresAt)}.", fontSize = 13.sp, color = Color.Gray)
            TextButton(onClick = { code = null }) { Text("Ocultar") }
        }
    }

    // --- Solicitudes pendientes
    if (pending.isNotEmpty()) {
        Section("Solicitudes")
        pending.forEach { r ->
            val sessionName = sessions.firstOrNull { it.id == r.sessionId }?.name ?: "una sesión"
            Column(Modifier.card()) {
                Text("${r.label ?: "Un ayudante"} quiere unirse a «$sessionName»", color = Color.Black)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        command({ client.acceptRequest(r.requestId) }) { code = code?.takeIf { it.sessionId != r.sessionId } }
                    }) { Text("Aceptar") }
                    OutlinedButton(onClick = { command({ client.rejectRequest(r.requestId) }) }) { Text("Rechazar") }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    // --- Sesiones
    Section("Sesiones")
    if (sessions.isEmpty()) {
        Text("Todavía no hay sesiones.", fontSize = 14.sp, color = Color.Gray)
    }
    sessions.forEach { s ->
        Column(Modifier.card()) {
            Text(s.name, fontSize = 18.sp, color = Color.Black)
            Text(
                when (s.status) {
                    SessionStatus.ACTIVE -> "Activa"
                    SessionStatus.PAUSED -> "Pausada"
                    SessionStatus.EXPIRED -> "Caducada"
                    SessionStatus.REVOKED -> "Revocada"
                },
                fontSize = 13.sp, color = if (s.status == SessionStatus.ACTIVE) Color(0xFF1B8A3A) else Color.Gray,
            )
            Text(
                s.expiresAt?.let { "Caduca el ${formatDateTime(it)}" } ?: "Sin caducidad",
                fontSize = 13.sp, color = Color.Gray,
            )
            Text(
                if (s.hasController) "Ayudante: ${s.controllerLabel ?: "emparejado"}" else "Sin ayudante emparejado",
                fontSize = 13.sp, color = Color.Gray,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                when (s.status) {
                    SessionStatus.PAUSED -> TextButton(onClick = { command({ client.activateSession(s.id) }) }) { Text("Activar") }
                    SessionStatus.ACTIVE -> TextButton(onClick = { command({ client.pauseSession(s.id) }) }) { Text("Pausar") }
                    else -> {}
                }
                if (s.status == SessionStatus.ACTIVE || s.status == SessionStatus.PAUSED) {
                    TextButton(onClick = {
                        command({ client.newPairingCode(s.id) }) { c -> code = c; codeFor = s.name }
                    }) { Text("Nuevo código") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (s.status != SessionStatus.REVOKED) {
                    TextButton(onClick = { confirm = Confirm(s, delete = false) }) { Text("Revocar") }
                }
                TextButton(onClick = { confirm = Confirm(s, delete = true) }) { Text("Borrar") }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    // --- Confirmación de revocar / borrar
    confirm?.let { c ->
        val id = c.session.id
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (c.delete) "¿Borrar la sesión?" else "¿Revocar la sesión?") },
            text = {
                Text(
                    if (c.delete) "Se borra «${c.session.name}» y sus ajustes del servidor."
                    else "El ayudante de «${c.session.name}» perderá el acceso para siempre. No se puede deshacer.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    val forget: (Any?) -> Unit = { code = code?.takeIf { it.sessionId != id } }
                    if (c.delete) {
                        // Al borrar, el servidor solo manda un ack al mago: se reconecta para refrescar la lista y los ajustes
                        command({ client.deleteSession(id) }) { forget(null); controller.refresh() }
                    } else {
                        command({ client.revokeSession(id) }, forget)
                    }
                }) { Text(if (c.delete) "Borrar" else "Revocar") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancelar") } },
        )
    }
}

private fun Modifier.card(): Modifier =
    fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFDDDDE3), RoundedCornerShape(12.dp)).padding(12.dp)

private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

private fun describeError(e: Throwable): String = when ((e as? MfException)?.code) {
    "not_connected" -> "Sin conexión con el servidor."
    "timeout" -> "El servidor no ha respondido."
    else -> e.message ?: "Error desconocido."
}

private fun two(n: Int) = if (n < 10) "0$n" else "$n"

private fun formatTime(ms: Long): String {
    val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${two(t.hour)}:${two(t.minute)}"
}

private fun formatDateTime(ms: Long): String {
    val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${two(t.dayOfMonth)}/${two(t.monthNumber)} ${two(t.hour)}:${two(t.minute)}"
}
