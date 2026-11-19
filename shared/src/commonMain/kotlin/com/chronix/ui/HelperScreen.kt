package com.chronix.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magicfriends.client.ConnectionState
import com.magicfriends.client.MfException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Cambia la lista de conexiones (la guarda en el móvil y la reordena). */
internal typealias UpdateConnections = ((List<HelperConnection>) -> List<HelperConnection>) -> Unit

/**
 * Pantalla secreta del **ayudante** (se abre desde los ajustes). Dos niveles, como la lista de sesiones del mago:
 * - **Lista de conexiones**: las sesiones a las que te has unido (del mismo mago o de varios), con su último estado, y «Unirme» con un código.
 * - **Una conexión** ([HelperSessionScreen]): se entra desde la lista, se conecta con ese mago y se ve el estado y la plantilla de esa sesión.
 * Sin pantalla no hay red: en la lista solo se usa internet al unirse o al salir de una conexión.
 */
@Composable
internal fun HelperScreen(prefs: KeyValueStore, onBack: () -> Unit) {
    val store = remember { HelperConnectionsStore(prefs) }
    var connections by remember { mutableStateOf(HelperRegistry.sorted(store.load())) }
    val update: UpdateConnections = { change ->
        val new = change(connections)
        store.save(new)
        connections = HelperRegistry.sorted(new)
    }
    var open by remember { mutableStateOf<String?>(null) } // clave de la conexión en la que se está trabajando
    val current = connections.firstOrNull { it.key == open }
    // Si la conexión desaparece de la lista (el ayudante se sale), se vuelve a la lista
    LaunchedEffect(current == null) { if (current == null) open = null }

    if (current != null) {
        HelperSessionScreen(prefs = prefs, conn = current, update = update, onBack = { open = null })
    } else {
        HelperList(prefs = prefs, connections = connections, update = update, onEnter = { open = it.key }, onBack = onBack)
    }
}

/** Marco común de las pantallas del ayudante: fondo blanco, margen seguro, «Volver» y título. */
@Composable
internal fun HelperFrame(backText: String, title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    val focus = LocalFocusManager.current
    Column(
        Modifier.fillMaxSize().background(Color.White).windowInsetsPadding(WindowInsets.safeDrawing)
            .pointerInput(Unit) { detectTapGestures { focus.clearFocus() } }
            .verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { focus.clearFocus(); onBack() }) { Text(backText, fontSize = 18.sp) }
            Spacer(Modifier.weight(1f))
        }
        Text(title, fontSize = 28.sp, color = Color.Black)
        content()
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun HelperList(
    prefs: KeyValueStore,
    connections: List<HelperConnection>,
    update: UpdateConnections,
    onEnter: (HelperConnection) -> Unit,
    onBack: () -> Unit,
) {
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    // Cliente sin mago: no conecta; solo sirve para canjear un código (la identidad del ayudante se crea al abrir esta pantalla)
    val joiner = remember { MagicFriendsHelper(prefs, scope) }

    var message by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("Ayudante") }
    var busy by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf<HelperConnection?>(null) }
    var offlineRemoval by remember { mutableStateOf<HelperConnection?>(null) }

    HelperFrame(backText = "‹ Volver", title = "Ayudante", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        message?.let { Text(it, fontSize = 14.sp, color = Color(0xFFC62828)) }
        info?.let { Text(it, fontSize = 14.sp, color = Color(0xFF1B8A3A)) }

        // --- Lista de conexiones
        Section("Mis conexiones")
        if (connections.isEmpty()) {
            Text("Todavía no estás unido a ninguna sesión. Pídele un código al mago y úsalo más abajo.", fontSize = 14.sp, color = Color.Gray)
        }
        connections.forEach { c ->
            Column(Modifier.card()) {
                Text(c.name, fontSize = 18.sp, color = Color.Black)
                Text(
                    HelperRegistry.statusText(c),
                    fontSize = 13.sp,
                    color = when (c.state) {
                        ConnState.ACTIVE -> Color(0xFF1B8A3A)
                        ConnState.REMOVED -> Color(0xFFC62828)
                        else -> Color.Gray
                    },
                )
                c.lastSeen?.let { Text("Visto el ${formatDateTime(it)}", fontSize = 12.sp, color = Color.Gray) }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (c.state != ConnState.REMOVED) TextButton(onClick = { onEnter(c) }) { Text("Entrar") }
                    TextButton(enabled = !busy, onClick = { leaving = c }) { Text(HelperRegistry.leaveLabel(c)) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // --- Unirse con un código
        Section("Unirme a una sesión")
        Text("Escribe el código que te ha dado el mago (6 caracteres, de un solo uso). Se añade a tu lista; puedes unirte a las sesiones que quieras, del mismo mago o de varios.",
            fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { raw -> code = raw.uppercase().filter { it.isLetterOrDigit() }.take(6) },
            singleLine = true, label = { Text("Código") }, modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 22.sp, letterSpacing = 4.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = label, onValueChange = { label = it.take(24) },
            singleLine = true, label = { Text("Tu nombre (lo verá el mago)") }, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        )
        Spacer(Modifier.height(8.dp))
        Button(
            enabled = code.length == 6 && !busy,
            onClick = {
                focus.clearFocus()
                busy = true
                scope.launch {
                    joiner.client.join(code, label.trim().ifBlank { null })
                        .onSuccess { j ->
                            update { HelperRegistry.joined(it, j.magoId, j.sessionId, j.sessionName, nowMs()) }
                            message = null
                            info = "Solicitud enviada a «${j.sessionName}». Cuando el mago la acepte, entra en ella desde la lista."
                            code = ""
                        }
                        .onFailure { message = describeHelperError(it); info = null }
                    busy = false
                }
            },
        ) { Text("Unirme") }
    }

    // --- Salir de una conexión / quitarla de la lista
    leaving?.let { c ->
        val onServer = HelperRegistry.canLeaveOnServer(c)
        val accepted = HelperRegistry.leaveLabel(c) == "Salir"
        AlertDialog(
            onDismissRequest = { leaving = null },
            title = { Text(if (accepted) "¿Salir de «${c.name}»?" else "¿Quitar «${c.name}» de la lista?") },
            text = {
                Text(
                    when {
                        accepted -> "Dejarás de controlar esta sesión y se borrará tu plantilla en ella: el móvil del mago volverá a sus propios ajustes. " +
                            "La sesión sigue siendo del mago; solo desaparece de tu lista."
                        onServer -> "Se quita de tu lista. Si el mago ya había aceptado tu solicitud, también sales de la sesión (hace falta conexión); " +
                            "si no, no se avisa a nadie."
                        else -> "Se quita de tu lista. El mago no recibe ningún aviso."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    leaving = null
                    if (!onServer) {
                        update { HelperRegistry.remove(it, c.magoId, c.sessionId) }
                    } else {
                        busy = true
                        info = "Saliendo de «${c.name}»…"
                        message = null
                        scope.launch {
                            val result = leaveFromList(prefs, scope, c)
                            info = null
                            val error = result.exceptionOrNull()
                            val errCode = (error as? MfException)?.code
                            if (error == null || errCode == "forbidden" || errCode == "not_found") {
                                // Salida hecha (o la sesión ya no existía para este dispositivo): fuera de la lista
                                update { HelperRegistry.remove(it, c.magoId, c.sessionId) }
                            } else {
                                message = describeHelperError(error)
                                offlineRemoval = c
                            }
                            busy = false
                        }
                    }
                }) { Text(if (accepted) "Salir" else "Quitar") }
            },
            dismissButton = { TextButton(onClick = { leaving = null }) { Text("Cancelar") } },
        )
    }
    offlineRemoval?.let { c ->
        AlertDialog(
            onDismissRequest = { offlineRemoval = null },
            title = { Text("No se pudo avisar al mago") },
            text = {
                Text("¿Quieres quitar «${c.name}» solo de tu lista? El mago seguirá viéndote como su ayudante hasta que la revoque o hasta que salgas de verdad (con conexión).")
            },
            confirmButton = {
                TextButton(onClick = {
                    offlineRemoval = null
                    update { HelperRegistry.remove(it, c.magoId, c.sessionId) }
                    message = null
                }) { Text("Quitar de la lista") }
            },
            dismissButton = { TextButton(onClick = { offlineRemoval = null }) { Text("Cancelar") } },
        )
    }
}

/**
 * Sale de una conexión desde la lista, sin haber entrado: abre un cliente con ese mago, espera a conectar y envía `session.leave`.
 * Si el servidor ya no nos reconoce (`bad_auth`) no hay nada que dejar: se da por hecho.
 */
private suspend fun leaveFromList(prefs: KeyValueStore, scope: CoroutineScope, c: HelperConnection): Result<Unit> {
    val helper = MagicFriendsHelper(prefs, scope, c.magoId)
    helper.start()
    try {
        val state = withTimeoutOrNull(OFFLINE_WAIT_MS) {
            helper.client.connection.first { it is ConnectionState.Connected || it is ConnectionState.AuthFailed }
        }
        return when (state) {
            null -> Result.failure(MfException("not_connected", "Sin conexión con el servidor"))
            is ConnectionState.AuthFailed -> Result.success(Unit)
            else -> {
                // Si la sesión no está entre las que controlamos (aún pendiente, o ya no existe), no hay nada que dejar en el servidor
                val controlled = helper.client.sessions.value.any { it.id == c.sessionId }
                if (controlled) helper.client.leaveSession(c.sessionId) else Result.success(Unit)
            }
        }
    } finally {
        helper.stop()
    }
}

private const val OFFLINE_WAIT_MS = 8_000L

internal fun describeHelperError(e: Throwable): String =
    (e as? MfException)?.let { describeHelperError(it.code, it.message ?: "") } ?: (e.message ?: "Error desconocido.")

/** Los errores del protocolo, en cristiano. */
internal fun describeHelperError(code: String, fallback: String): String = when (code) {
    "bad_code" -> "Código no válido o ya usado. Pídele otro al mago."
    "expired" -> "La sesión ha caducado."
    "not_active" -> "La sesión no admite cambios ahora mismo."
    "forbidden" -> "No controlas esa sesión."
    "rate_limited" -> "Demasiados intentos. Espera un momento."
    "limit_reached" -> "La sesión ya tiene el máximo de ayudantes."
    "not_connected" -> "Sin conexión con el servidor."
    "timeout" -> "El servidor no ha respondido."
    "bad_auth" -> "El servidor no reconoce este dispositivo."
    else -> fallback.ifBlank { code }
}
