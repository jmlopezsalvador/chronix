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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.magicfriends.client.ClientEvent
import com.magicfriends.client.ConnectionState
import com.magicfriends.client.MfException
import com.magicfriends.client.SessionStatus
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/**
 * Pantalla secreta del **ayudante** (se abre desde los ajustes): se une a la sesión de un mago con su código y le cambia en
 * remoto, con el mismo editor de forzaje de los ajustes, el plan que usará su móvil. Es un fondo blanco como el resto de los
 * ajustes. La conexión (y la identidad del ayudante) solo existen mientras esta pantalla está abierta.
 */
@Composable
internal fun HelperScreen(prefs: KeyValueStore, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val helper = remember { MagicFriendsHelper(prefs, scope) }
    DisposableEffect(helper) {
        helper.start()
        onDispose { helper.stop() }
    }
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
        Text("Ayudante", fontSize = 28.sp, color = Color.Black)
        HelperContent(helper)
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun HelperContent(helper: MagicFriendsHelper) {
    val client = helper.client
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val connection by client.connection.collectAsState()
    val sessions by client.sessions.collectAsState()
    val pending by client.pending.collectAsState()
    val helperState by client.helperState.collectAsState()

    var message by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("Ayudante") }
    var joined by remember { mutableStateOf(false) } // acaba de pedir unirse desde esta pantalla
    var busy by remember { mutableStateOf(false) }
    var showJoin by remember { mutableStateOf(false) }

    // Avisos del servidor: solicitud rechazada, acceso retirado…
    LaunchedEffect(client) {
        client.events.collect { event ->
            when (event) {
                is ClientEvent.PairingResult -> {
                    if (event.accepted) message = null else { message = "El mago ha rechazado tu solicitud."; joined = false }
                }
                is ClientEvent.SessionRemoved -> {
                    message = when (event.reason) {
                        "revoked" -> "El mago ha revocado tu acceso."
                        "deleted" -> "El mago ha borrado la sesión."
                        "replaced" -> "Otro ayudante ha ocupado tu lugar en la sesión."
                        else -> "Ya no controlas esa sesión."
                    }
                    joined = false
                }
                is ClientEvent.ServerError -> message = describeHelperError(event.code, event.message)
                else -> {}
            }
        }
    }

    val session = sessions.firstOrNull { it.status == SessionStatus.ACTIVE } ?: sessions.firstOrNull()
    val connected = connection is ConnectionState.Connected

    Spacer(Modifier.height(8.dp))
    Text(
        when (connection) {
            ConnectionState.Connected -> "Conectado"
            ConnectionState.Connecting -> "Conectando…"
            ConnectionState.Disconnected -> "Sin conexión (se reintenta solo)"
            ConnectionState.AuthFailed -> "Tu acceso ya no es válido: pídele un código nuevo al mago"
            ConnectionState.Idle -> "Sin emparejar"
        },
        fontSize = 14.sp, color = if (connected) Color(0xFF1B8A3A) else Color.Gray,
    )
    message?.let { Text(it, fontSize = 14.sp, color = Color(0xFFC62828), modifier = Modifier.padding(top = 4.dp)) }

    // --- Unirse con un código (siempre que no haya sesión; con sesión, solo si se pide)
    val joinForm: @Composable () -> Unit = {
        Section("Unirse a una sesión")
        Text("Escribe el código que te ha dado el mago (6 caracteres, de un solo uso). Quedas ligado a un solo mago a la vez.",
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
                    client.join(code, label.trim().ifBlank { null })
                        .onSuccess { joined = true; message = null; info = null; code = ""; showJoin = false }
                        .onFailure { message = describeHelperError(it) }
                    busy = false
                }
            },
        ) { Text("Unirme") }
    }

    if (session == null) {
        if (pending.isNotEmpty() || joined) {
            Spacer(Modifier.height(12.dp))
            Text("Solicitud enviada. Esperando a que el mago la acepte…", fontSize = 15.sp, color = Color.Black)
        }
        joinForm()
        return
    }

    // --- Sesión que controla
    Section("Sesión «${session.name}»")
    val active = session.status == SessionStatus.ACTIVE
    Text(
        when (session.status) {
            SessionStatus.ACTIVE -> "Activa: lo que envíes llega al móvil del mago."
            SessionStatus.PAUSED -> "Pausada: el mago todavía no la ha activado (o la ha pausado). Puedes ir preparando el plan; no se envía hasta que la active."
            SessionStatus.EXPIRED -> "Caducada: ya no se puede enviar nada."
            SessionStatus.REVOKED -> "Revocada."
        },
        fontSize = 14.sp, color = if (active) Color(0xFF1B8A3A) else Color.Gray,
    )
    session.expiresAt?.let { Text("Caduca el ${formatDateTime(it)}", fontSize = 13.sp, color = Color.Gray) }

    val remote = helperState[session.id]
    val remoteSettings = remote?.settings ?: JsonObject(emptyMap())
    val remoteValues = parseRemoteOverrides(remoteSettings).toForcingValues()
    val controlling = remoteSettings.hasFullForcingPlan()

    // El borrador sigue lo que haya en el servidor mientras el ayudante no lo haya tocado
    var draft by remember(session.id) { mutableStateOf(remoteValues) }
    var touched by remember(session.id) { mutableStateOf(false) }
    LaunchedEffect(remoteValues, touched) { if (!touched) draft = remoteValues }
    val dirty = draft != remoteValues

    Spacer(Modifier.height(8.dp))
    Text(
        if (controlling) "Controlando: el móvil del mago usa tu plan (rev ${remote?.rev})."
        else "Todavía no has enviado tu plan: el móvil del mago usa sus propios ajustes.",
        fontSize = 14.sp, color = Color.Black,
    )

    ForcingEditor(
        values = draft,
        onChange = { draft = it; touched = true; info = null },
        remote = true,
    )

    Spacer(Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            enabled = active && connected && (!controlling || dirty),
            onClick = {
                focus.clearFocus()
                scope.launch {
                    client.sendPatch(session.id, draft.toPatch())
                        .onSuccess { rev -> message = null; info = "Enviado (rev $rev)."; touched = false }
                        .onFailure { message = describeHelperError(it) }
                }
            },
        ) { Text("Enviar") }
        OutlinedButton(
            enabled = active && connected && controlling,
            onClick = {
                focus.clearFocus()
                scope.launch {
                    client.sendPatch(session.id, releasePatch())
                        .onSuccess { message = null; info = "Control soltado: el móvil del mago vuelve a sus ajustes."; touched = true }
                        .onFailure { message = describeHelperError(it) }
                }
            },
        ) { Text("Soltar el control") }
    }
    if (!active || !connected) {
        Text(
            if (!connected) "Hace falta conexión para enviar." else "Solo se puede enviar con la sesión activa.",
            fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp),
        )
    } else if (controlling && dirty) {
        Text("Hay cambios sin enviar.", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
    }
    info?.let { Text(it, fontSize = 14.sp, color = Color(0xFF1B8A3A), modifier = Modifier.padding(top = 4.dp)) }

    // --- Otro código (cambia de mago)
    Spacer(Modifier.height(16.dp))
    if (showJoin) {
        joinForm()
        TextButton(onClick = { showJoin = false }) { Text("Cancelar") }
    } else {
        TextButton(onClick = { showJoin = true }) { Text("Unirme con otro código") }
    }
}

private fun describeHelperError(e: Throwable): String =
    (e as? MfException)?.let { describeHelperError(it.code, it.message ?: "") } ?: (e.message ?: "Error desconocido.")

/** Los errores del protocolo, en cristiano. */
private fun describeHelperError(code: String, fallback: String): String = when (code) {
    "bad_code" -> "Código no válido o ya usado. Pídele otro al mago."
    "expired" -> "La sesión ha caducado."
    "not_active" -> "El mago aún no ha activado la sesión."
    "forbidden" -> "No controlas esa sesión."
    "rate_limited" -> "Demasiados intentos. Espera un momento."
    "limit_reached" -> "La sesión ya tiene el máximo de ayudantes."
    "not_connected" -> "Sin conexión con el servidor."
    "timeout" -> "El servidor no ha respondido."
    "bad_auth" -> "El servidor no reconoce este dispositivo."
    else -> fallback.ifBlank { code }
}
