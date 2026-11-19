package com.chronix.ui

import com.magicfriends.client.PendingRequest
import com.magicfriends.client.Session
import com.magicfriends.client.SessionStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** Estado que se muestra de una conexión del ayudante (lo último que se supo del servidor). */
enum class ConnState { PENDING, ACTIVE, PAUSED, EXPIRED, REMOVED }

/**
 * Una conexión del ayudante: una sesión de un mago a la que se ha unido (del mismo mago o de otros). Es el equivalente, para
 * el ayudante, de la lista de sesiones del mago. La lista vive en el móvil (el servidor solo sabe, mago a mago, qué
 * sesiones controla cada dispositivo) y se refresca cada vez que se entra en una conexión.
 *
 * Cada conexión tiene **su** plantilla (los ajustes guardados en esa sesión del servidor), así que cambiar de una a otra no pierde nada.
 */
data class HelperConnection(
    val magoId: String,
    val sessionId: String,
    val name: String,
    val state: ConnState,
    /** Si [state] es [ConnState.REMOVED]: por qué (`rejected`, `deleted`, `revoked`, `replaced`, `lost`). */
    val reason: String? = null,
    /** Última vez (epoch ms) que se supo su estado del servidor. */
    val lastSeen: Long? = null,
) {
    val key: String get() = "$magoId/$sessionId"
}

/**
 * Lógica pura de la lista de conexiones del ayudante (sin UI ni red, con el reloj inyectado): alta al unirse, refresco con lo que
 * dice el servidor, bajas por decisión del mago, y (de)serialización en el almacén local.
 */
internal object HelperRegistry {
    // ---- Almacén local (JSON) ----

    fun encode(list: List<HelperConnection>): String = JsonArray(list.map { c ->
        buildJsonObject {
            put("magoId", c.magoId)
            put("sessionId", c.sessionId)
            put("name", c.name)
            put("state", c.state.name)
            c.reason?.let { put("reason", it) }
            c.lastSeen?.let { put("lastSeen", it) }
        }
    }).toString()

    /** Tolerante: lo ilegible o incompleto se descarta en vez de romper la pantalla. */
    fun decode(text: String?): List<HelperConnection> {
        if (text.isNullOrBlank()) return emptyList()
        val array = try { Json.parseToJsonElement(text) as? JsonArray } catch (e: Exception) { null } ?: return emptyList()
        return array.mapNotNull { element ->
            val o = element as? JsonObject ?: return@mapNotNull null
            fun str(key: String) = (o[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
            val magoId = str("magoId") ?: return@mapNotNull null
            val sessionId = str("sessionId") ?: return@mapNotNull null
            val state = ConnState.entries.firstOrNull { it.name == str("state") } ?: return@mapNotNull null
            HelperConnection(
                magoId = magoId,
                sessionId = sessionId,
                name = str("name") ?: "Sesión",
                state = state,
                reason = str("reason"),
                lastSeen = (o["lastSeen"] as? JsonPrimitive)?.takeIf { !it.isString }?.longOrNull,
            )
        }
    }

    // ---- Cambios de la lista ----

    /** El ayudante acaba de canjear un código: la conexión queda pendiente de que el mago acepte (o se reemplaza la que hubiera). */
    fun joined(list: List<HelperConnection>, magoId: String, sessionId: String, name: String, now: Long): List<HelperConnection> {
        val new = HelperConnection(magoId, sessionId, name, ConnState.PENDING, lastSeen = now)
        return if (list.any { it.magoId == magoId && it.sessionId == sessionId }) {
            list.map { if (it.magoId == magoId && it.sessionId == sessionId) new else it }
        } else list + new
    }

    /**
     * Refresca las conexiones de un mago con lo que dice el servidor **tras conectar** (hay que llamarlo solo con la conexión
     * establecida: antes de la bienvenida la lista de sesiones está vacía y se daría todo por perdido). Las sesiones que el servidor
     * muestra y no estaban en la lista se añaden; las que ya no están (ni como solicitud pendiente) pasan a «retirada».
     */
    fun applySessions(
        list: List<HelperConnection>,
        magoId: String,
        sessions: List<Session>,
        pending: List<PendingRequest>,
        now: Long,
    ): List<HelperConnection> {
        val refreshed = list.map { c ->
            if (c.magoId != magoId) return@map c
            val s = sessions.firstOrNull { it.id == c.sessionId }
            when {
                s != null -> c.copy(name = s.name, state = s.status.toConnState(), reason = if (s.status == SessionStatus.REVOKED) "revoked" else null, lastSeen = now)
                pending.any { it.sessionId == c.sessionId } -> c.copy(state = ConnState.PENDING, reason = null, lastSeen = now)
                c.state == ConnState.REMOVED -> c
                else -> c.copy(state = ConnState.REMOVED, reason = "lost", lastSeen = now)
            }
        }
        val added = sessions
            .filter { s -> list.none { it.magoId == magoId && it.sessionId == s.id } }
            .map { s -> HelperConnection(magoId, s.id, s.name, s.status.toConnState(), if (s.status == SessionStatus.REVOKED) "revoked" else null, now) }
        return refreshed + added
    }

    /**
     * El servidor dice que ya no controlamos esa sesión. Si nos salimos nosotros (`left`) desaparece de la lista; si la retira el mago
     * (`deleted`, `revoked`) o la ocupa otro ayudante (`replaced`), queda marcada para que se vea por qué y se quite con un toque.
     */
    fun removedBy(list: List<HelperConnection>, magoId: String, sessionId: String, reason: String): List<HelperConnection> =
        if (reason == "left") remove(list, magoId, sessionId)
        else list.map { if (it.magoId == magoId && it.sessionId == sessionId) it.copy(state = ConnState.REMOVED, reason = reason) else it }

    /** El mago ha rechazado la solicitud. Sin id de sesión, se rechazan las pendientes de ese mago. */
    fun rejected(list: List<HelperConnection>, magoId: String, sessionId: String?): List<HelperConnection> =
        list.map { c ->
            val matches = c.magoId == magoId && c.state == ConnState.PENDING && (sessionId == null || c.sessionId == sessionId)
            if (matches) c.copy(state = ConnState.REMOVED, reason = "rejected") else c
        }

    /** El servidor no reconoce a este dispositivo con ese mago (sin sesiones ni solicitudes): todas sus conexiones están perdidas. */
    fun authLost(list: List<HelperConnection>, magoId: String): List<HelperConnection> =
        list.map { if (it.magoId == magoId && it.state != ConnState.REMOVED) it.copy(state = ConnState.REMOVED, reason = "lost") else it }

    fun remove(list: List<HelperConnection>, magoId: String, sessionId: String): List<HelperConnection> =
        list.filterNot { it.magoId == magoId && it.sessionId == sessionId }

    /** Activas primero, luego pausadas, pendientes, caducadas y retiradas; dentro de cada grupo, por nombre. */
    fun sorted(list: List<HelperConnection>): List<HelperConnection> {
        fun rank(s: ConnState) = when (s) {
            ConnState.ACTIVE -> 0
            ConnState.PAUSED -> 1
            ConnState.PENDING -> 2
            ConnState.EXPIRED -> 3
            ConnState.REMOVED -> 4
        }
        return list.sortedWith(compareBy<HelperConnection> { rank(it.state) }.thenBy { it.name.lowercase() })
    }

    /**
     * ¿Hay que intentar salir de esa conexión en el servidor? Sí en todas salvo las ya retiradas. Una **pendiente** también: lo que
     * dice la lista es lo último que se vio, y puede que el mago ya la haya aceptado (en ese caso se sale de verdad; si no, no hay nada
     * que dejar y solo se quita de la lista).
     */
    fun canLeaveOnServer(c: HelperConnection): Boolean = c.state != ConnState.REMOVED

    /** Texto del botón de la tarjeta: «Salir» si la sesión está aceptada y «Quitar» si no está confirmada o ya está retirada. */
    fun leaveLabel(c: HelperConnection): String = when (c.state) {
        ConnState.ACTIVE, ConnState.PAUSED, ConnState.EXPIRED -> "Salir"
        ConnState.PENDING, ConnState.REMOVED -> "Quitar"
    }

    /** Texto del estado de una conexión para la lista. */
    fun statusText(c: HelperConnection): String = when (c.state) {
        ConnState.ACTIVE -> "Activa"
        ConnState.PAUSED -> "Pausada"
        ConnState.EXPIRED -> "Caducada"
        ConnState.PENDING -> "Esperando a que el mago acepte"
        ConnState.REMOVED -> when (c.reason) {
            "rejected" -> "El mago rechazó la solicitud"
            "deleted" -> "El mago borró la sesión"
            "revoked" -> "El mago revocó tu acceso"
            "replaced" -> "Otro ayudante ocupó tu lugar"
            else -> "El mago ya no te tiene en esta sesión"
        }
    }

    private fun SessionStatus.toConnState(): ConnState = when (this) {
        SessionStatus.ACTIVE -> ConnState.ACTIVE
        SessionStatus.PAUSED -> ConnState.PAUSED
        SessionStatus.EXPIRED -> ConnState.EXPIRED
        SessionStatus.REVOKED -> ConnState.REMOVED
    }
}

/** Almacén local de la lista de conexiones (misma `KeyValueStore` que los ajustes, con su propia clave). */
internal class HelperConnectionsStore(private val prefs: KeyValueStore) {
    fun load(): List<HelperConnection> = HelperRegistry.decode(prefs.getString(KEY))
    fun save(list: List<HelperConnection>) = prefs.putString(KEY, HelperRegistry.encode(list))

    private companion object { const val KEY = "mfh.connections" }
}

/** Qué se puede hacer con el plan del ayudante según el estado de la sesión (la regla de los botones). */
internal data class PlanActions(
    val canSend: Boolean,
    val canRelease: Boolean,
    val sendLabel: String,
    val releaseLabel: String,
    /** Por qué no se puede enviar, si hay un motivo que decir. */
    val note: String?,
)

/**
 * Reglas de la plantilla (decididas el 2026-11-19): se puede editar y enviar con la sesión **activa** (llega ya al móvil del mago) y con
 * la **pausada** (se guarda y el mago la usará al activarla); nunca con la caducada. Una vez hay un plan, «enviar» solo vale con cambios.
 */
internal fun planActions(status: SessionStatus, connected: Boolean, hasPlan: Boolean, dirty: Boolean): PlanActions {
    val editable = status == SessionStatus.ACTIVE || status == SessionStatus.PAUSED
    val active = status == SessionStatus.ACTIVE
    return PlanActions(
        canSend = connected && editable && (!hasPlan || dirty),
        canRelease = connected && editable && hasPlan,
        sendLabel = if (active) "Enviar" else "Guardar plantilla",
        releaseLabel = if (active) "Soltar el control" else "Vaciar plantilla",
        note = when {
            !connected -> "Hace falta conexión para guardar cambios."
            status == SessionStatus.EXPIRED -> "La sesión ha caducado: no se puede cambiar nada hasta que el mago la reactive."
            status == SessionStatus.REVOKED -> "La sesión está revocada."
            else -> null
        },
    )
}
