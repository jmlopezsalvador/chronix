package com.chronix.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magicfriends.client.ClientEvent
import com.magicfriends.client.ConnectionState
import com.magicfriends.client.MagicFriendsClient
import com.magicfriends.client.MfException
import com.magicfriends.client.RemoteState
import com.magicfriends.client.Session
import com.magicfriends.client.SessionStatus
import kotlinx.coroutines.launch

/**
 * Una conexión del ayudante: se conecta con el mago de esa sesión (un cliente por mago) y enseña su estado y **su plantilla**, que
 * se edita con el mismo editor de forzaje de los ajustes. La conexión solo existe mientras esta pantalla está abierta. Cada vez que
 * se conecta, refresca el estado de la lista de conexiones con lo que dice el servidor.
 */
@Composable
internal fun HelperSessionScreen(prefs: KeyValueStore, conn: HelperConnection, update: UpdateConnections, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val helper = remember(conn.magoId) { MagicFriendsHelper(prefs, scope, conn.magoId) }
    DisposableEffect(helper) {
        helper.start()
        onDispose { helper.stop() }
    }
    HelperFrame(backText = "‹ Mis conexiones", title = conn.name, onBack = onBack) {
        SessionContent(helper, conn, update, onBack)
    }
}

@Composable
private fun SessionContent(helper: MagicFriendsHelper, conn: HelperConnection, update: UpdateConnections, onBack: () -> Unit) {
    val client = helper.client
    val scope = rememberCoroutineScope()
    val connection by client.connection.collectAsState()
    val sessions by client.sessions.collectAsState()
    val pending by client.pending.collectAsState()
    val helperState by client.helperState.collectAsState()

    var message by remember { mutableStateOf<String?>(null) }
    var leaveDialog by remember { mutableStateOf(false) }
    var offlineRemoval by remember { mutableStateOf(false) }

    // Refresca la lista con lo que dice el servidor. Solo con la conexión establecida: antes de la bienvenida las sesiones están
    // vacías y se daría todo por perdido.
    LaunchedEffect(connection, sessions, pending) {
        if (connection is ConnectionState.Connected) update { HelperRegistry.applySessions(it, conn.magoId, sessions, pending, nowMs()) }
        if (connection is ConnectionState.AuthFailed) update { HelperRegistry.authLost(it, conn.magoId) }
    }
    LaunchedEffect(client) {
        client.events.collect { event ->
            when (event) {
                is ClientEvent.PairingResult -> {
                    if (!event.accepted) update { HelperRegistry.rejected(it, conn.magoId, event.sessionId) }
                }
                is ClientEvent.SessionRemoved -> update { HelperRegistry.removedBy(it, conn.magoId, event.sessionId, event.reason) }
                // `bad_auth` ya lo cuenta el estado de la conexión («el servidor ya no te reconoce»): no se repite en rojo
                is ClientEvent.ServerError -> if (event.code != "bad_auth") message = describeHelperError(event.code, event.message)
                else -> {}
            }
        }
    }

    val session = sessions.firstOrNull { it.id == conn.sessionId }
    val connected = connection is ConnectionState.Connected

    Spacer(Modifier.height(8.dp))
    Text(
        when (connection) {
            ConnectionState.Connected -> "Conectado"
            ConnectionState.Connecting -> "Conectando…"
            ConnectionState.Disconnected -> "Sin conexión (se reintenta solo)"
            ConnectionState.AuthFailed -> "El servidor ya no te reconoce con este mago"
            ConnectionState.Idle -> "Sin conectar"
        },
        fontSize = 14.sp, color = if (connected) Color(0xFF1B8A3A) else Color.Gray,
    )
    message?.let { Text(it, fontSize = 14.sp, color = Color(0xFFC62828), modifier = Modifier.padding(top = 4.dp)) }

    // --- La conexión ya no es válida (rechazada, retirada, borrada…): se ve por qué y se quita con un toque
    if (conn.state == ConnState.REMOVED) {
        Spacer(Modifier.height(16.dp))
        Text(HelperRegistry.statusText(conn), fontSize = 16.sp, color = Color(0xFFC62828))
        Spacer(Modifier.height(12.dp))
        Button(onClick = { update { HelperRegistry.remove(it, conn.magoId, conn.sessionId) }; onBack() }) { Text("Quitar de mi lista") }
        return
    }

    // --- Todavía sin sesión: pendiente de aceptación, o conectando
    if (session == null) {
        Spacer(Modifier.height(16.dp))
        Text(
            when {
                conn.state == ConnState.PENDING || pending.any { it.sessionId == conn.sessionId } -> "Esperando a que el mago acepte tu solicitud…"
                connected -> "Esta sesión ya no aparece en el servidor."
                else -> "Cargando la sesión…"
            },
            fontSize = 15.sp, color = Color.Black,
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { update { HelperRegistry.remove(it, conn.magoId, conn.sessionId) }; onBack() }) { Text("Quitar de mi lista") }
        return
    }

    // --- Estado de la sesión
    Section("Estado")
    Text(
        when (session.status) {
            SessionStatus.ACTIVE -> "Activa: lo que envíes llega al móvil del mago."
            SessionStatus.PAUSED -> "Pausada: puedes dejar la plantilla preparada; el mago la usará cuando active la sesión."
            SessionStatus.EXPIRED -> "Caducada."
            SessionStatus.REVOKED -> "Revocada."
        },
        fontSize = 14.sp, color = if (session.status == SessionStatus.ACTIVE) Color(0xFF1B8A3A) else Color.Gray,
    )
    session.expiresAt?.let { Text("Caduca el ${formatDateTime(it)}", fontSize = 13.sp, color = Color.Gray) }

    // --- Plantilla de esta conexión (los ajustes guardados en la sesión)
    val remote = helperState[session.id]
    if (remote == null) {
        Spacer(Modifier.height(12.dp))
        Text("Cargando la plantilla…", fontSize = 14.sp, color = Color.Gray)
    } else {
        PlanEditor(client, session, remote, connected)
    }

    // --- Salir de la sesión (eliminarla de la lista)
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = { leaveDialog = true }) { Text("Salir de la sesión") }

    if (leaveDialog) {
        AlertDialog(
            onDismissRequest = { leaveDialog = false },
            title = { Text("¿Salir de «${session.name}»?") },
            text = {
                Text("Dejarás de controlar esta sesión y se borrará tu plantilla en ella: el móvil del mago volverá a sus propios ajustes. " +
                    "La sesión sigue siendo del mago; solo desaparece de tu lista.")
            },
            confirmButton = {
                TextButton(onClick = {
                    leaveDialog = false
                    scope.launch {
                        val error = client.leaveSession(session.id).exceptionOrNull()
                        val errCode = (error as? MfException)?.code
                        if (error == null || errCode == "forbidden" || errCode == "not_found") {
                            update { HelperRegistry.remove(it, conn.magoId, conn.sessionId) }
                            onBack()
                        } else {
                            message = describeHelperError(error)
                            offlineRemoval = true
                        }
                    }
                }) { Text("Salir") }
            },
            dismissButton = { TextButton(onClick = { leaveDialog = false }) { Text("Cancelar") } },
        )
    }
    if (offlineRemoval) {
        AlertDialog(
            onDismissRequest = { offlineRemoval = false },
            title = { Text("No se pudo avisar al mago") },
            text = {
                Text("¿Quieres quitar «${session.name}» solo de tu lista? El mago seguirá viéndote como su ayudante hasta que la revoque o hasta que salgas de verdad (con conexión).")
            },
            confirmButton = {
                TextButton(onClick = {
                    offlineRemoval = false
                    update { HelperRegistry.remove(it, conn.magoId, conn.sessionId) }
                    onBack()
                }) { Text("Quitar de la lista") }
            },
            dismissButton = { TextButton(onClick = { offlineRemoval = false }) { Text("Cancelar") } },
        )
    }
}

/**
 * El editor de la plantilla de una sesión. Plan completo (las seis claves) con botón explícito, como en la fase 2; con la sesión
 * pausada se **guarda** (el mago la usará al activarla) y con la activa se **envía** (llega ya al móvil del mago).
 */
@Composable
private fun PlanEditor(client: MagicFriendsClient, session: Session, remote: RemoteState, connected: Boolean) {
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var message by remember(session.id) { mutableStateOf<String?>(null) }
    var info by remember(session.id) { mutableStateOf<String?>(null) }

    val remoteValues = parseRemoteOverrides(remote.settings).toForcingValues()
    val hasPlan = remote.settings.hasFullForcingPlan()
    // El borrador sigue lo que haya en el servidor mientras el ayudante no lo haya tocado
    var draft by remember(session.id) { mutableStateOf(remoteValues) }
    var touched by remember(session.id) { mutableStateOf(false) }
    LaunchedEffect(remoteValues, touched) { if (!touched) draft = remoteValues }
    val dirty = draft != remoteValues
    val active = session.status == SessionStatus.ACTIVE
    val actions = planActions(session.status, connected, hasPlan, dirty)

    Section("Plantilla de esta conexión")
    Text(
        when {
            hasPlan && active -> "Controlando: el móvil del mago usa tu plantilla (rev ${remote.rev})."
            hasPlan -> "Plantilla guardada (rev ${remote.rev})."
            active -> "Todavía no has enviado tu plan: el móvil del mago usa sus propios ajustes."
            else -> "Esta sesión aún no tiene plantilla: si el mago la activa así, usará sus propios ajustes."
        },
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
            enabled = actions.canSend,
            onClick = {
                focus.clearFocus()
                scope.launch {
                    client.sendPatch(session.id, draft.toPatch())
                        .onSuccess { rev -> message = null; info = if (active) "Enviado (rev $rev)." else "Plantilla guardada (rev $rev)."; touched = false }
                        .onFailure { message = describeHelperError(it) }
                }
            },
        ) { Text(actions.sendLabel) }
        OutlinedButton(
            enabled = actions.canRelease,
            onClick = {
                focus.clearFocus()
                scope.launch {
                    client.sendPatch(session.id, releasePatch())
                        .onSuccess {
                            message = null
                            info = if (active) "Control soltado: el móvil del mago vuelve a sus ajustes." else "Plantilla vaciada."
                            touched = true // se conserva el borrador para poder volver a guardarlo
                        }
                        .onFailure { message = describeHelperError(it) }
                }
            },
        ) { Text(actions.releaseLabel) }
    }
    val note = actions.note ?: if (hasPlan && dirty) "Hay cambios sin guardar." else null
    note?.let { Text(it, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp)) }
    message?.let { Text(it, fontSize = 14.sp, color = Color(0xFFC62828), modifier = Modifier.padding(top = 4.dp)) }
    info?.let { Text(it, fontSize = 14.sp, color = Color(0xFF1B8A3A), modifier = Modifier.padding(top = 4.dp)) }
}
