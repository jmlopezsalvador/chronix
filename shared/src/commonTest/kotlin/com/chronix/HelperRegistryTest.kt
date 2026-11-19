package com.chronix

import com.chronix.ui.ConnState
import com.chronix.ui.HelperConnection
import com.chronix.ui.HelperConnectionsStore
import com.chronix.ui.HelperRegistry
import com.chronix.ui.KeyValueStore
import com.chronix.ui.planActions
import com.magicfriends.client.PendingRequest
import com.magicfriends.client.Session
import com.magicfriends.client.SessionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** La lista de conexiones del ayudante (sesiones a las que se ha unido, de uno o de varios magos) y las reglas de la plantilla. */
class HelperRegistryTest {
    private val now = 1_000L

    private fun session(id: String, name: String = "Sesión $id", status: SessionStatus = SessionStatus.PAUSED) =
        Session(id = id, name = name, status = status, hasController = true, controllerLabel = "yo", rev = 0, createdAt = 0, updatedAt = 0)

    private fun pending(sessionId: String) = PendingRequest(requestId = "r-$sessionId", sessionId = sessionId, label = "yo", at = 0)

    private fun conn(mago: String, session: String, state: ConnState, name: String = "S-$session", reason: String? = null, lastSeen: Long? = null) =
        HelperConnection(mago, session, name, state, reason, lastSeen)

    // ---- Almacén local ----

    @Test fun encodeAndDecodeRoundTripEveryState() {
        val list = listOf(
            conn("m1", "a", ConnState.ACTIVE, "Función del sábado", lastSeen = 5L),
            conn("m1", "b", ConnState.PAUSED),
            conn("m2", "c", ConnState.PENDING),
            conn("m2", "d", ConnState.EXPIRED, lastSeen = 7L),
            conn("m3", "e", ConnState.REMOVED, reason = "deleted", lastSeen = 9L),
        )
        assertEquals(list, HelperRegistry.decode(HelperRegistry.encode(list)))
    }

    @Test fun decodeIsTolerantAndNeverBreaksTheScreen() {
        assertEquals(emptyList(), HelperRegistry.decode(null))
        assertEquals(emptyList(), HelperRegistry.decode(""))
        assertEquals(emptyList(), HelperRegistry.decode("   "))
        assertEquals(emptyList(), HelperRegistry.decode("esto no es json"))
        assertEquals(emptyList(), HelperRegistry.decode("{\"a\":1}")) // no es un array
        // Entradas incompletas o con estado desconocido se descartan; las buenas se conservan; sin nombre, uno por defecto
        val text = """[
            {"magoId":"m1","sessionId":"a","name":"Buena","state":"PAUSED"},
            {"magoId":"m1","state":"PAUSED"},
            {"magoId":"m1","sessionId":"b","name":"Rara","state":"NO_EXISTE"},
            {"magoId":"m2","sessionId":"c","state":"ACTIVE"},
            5
        ]"""
        val decoded = HelperRegistry.decode(text)
        assertEquals(listOf("a", "c"), decoded.map { it.sessionId })
        assertEquals("Sesión", decoded[1].name)
    }

    @Test fun theStoreSavesAndLoadsTheListInTheSameStoreAsTheSettings() {
        val prefs = MemoryStore()
        val store = HelperConnectionsStore(prefs)
        assertEquals(emptyList(), store.load())
        val list = listOf(conn("m1", "a", ConnState.ACTIVE, lastSeen = 3L))
        store.save(list)
        assertEquals(list, HelperConnectionsStore(prefs).load()) // sobrevive a recrear el almacén (reiniciar la app)
    }

    // ---- Unirse ----

    @Test fun joiningAddsAPendingConnectionWithTheNameFromTheServer() {
        val list = HelperRegistry.joined(emptyList(), "m1", "a", "Función del sábado", now)
        assertEquals(listOf(conn("m1", "a", ConnState.PENDING, "Función del sábado", lastSeen = now)), list)
    }

    @Test fun joiningAgainTheSameSessionReplacesItInPlaceAndOtherMagosAreIndependent() {
        val start = listOf(conn("m1", "a", ConnState.REMOVED, reason = "revoked"), conn("m2", "a", ConnState.ACTIVE))
        val list = HelperRegistry.joined(start, "m1", "a", "Nueva", now)
        assertEquals(2, list.size)
        assertEquals(ConnState.PENDING, list[0].state)
        assertNull(list[0].reason)
        assertEquals("Nueva", list[0].name)
        assertEquals(ConnState.ACTIVE, list[1].state) // la sesión «a» de OTRO mago es otra conexión
    }

    @Test fun joiningSeveralSessionsOfTheSameOrDifferentMagosKeepsAllOfThem() {
        var list = emptyList<HelperConnection>()
        list = HelperRegistry.joined(list, "m1", "a", "A", now)
        list = HelperRegistry.joined(list, "m1", "b", "B", now)
        list = HelperRegistry.joined(list, "m2", "c", "C", now)
        assertEquals(listOf("m1/a", "m1/b", "m2/c"), list.map { it.key })
    }

    // ---- Refrescar con lo que dice el servidor ----

    @Test fun refreshingTurnsPendingIntoTheRealStateAndUpdatesNameAndLastSeen() {
        val list = listOf(conn("m1", "a", ConnState.PENDING, "viejo"), conn("m1", "b", ConnState.PAUSED))
        val sessions = listOf(session("a", "Gala", SessionStatus.PAUSED), session("b", "Show", SessionStatus.ACTIVE))
        val out = HelperRegistry.applySessions(list, "m1", sessions, emptyList(), now)
        assertEquals(ConnState.PAUSED, out[0].state)
        assertEquals("Gala", out[0].name)
        assertEquals(ConnState.ACTIVE, out[1].state)
        assertTrue(out.all { it.lastSeen == now })
    }

    @Test fun refreshingOneMagoNeverTouchesTheConnectionsOfAnother() {
        val list = listOf(conn("m1", "a", ConnState.ACTIVE, lastSeen = 1L), conn("m2", "z", ConnState.ACTIVE, lastSeen = 1L))
        val out = HelperRegistry.applySessions(list, "m1", emptyList(), emptyList(), now)
        assertEquals(ConnState.REMOVED, out.first { it.magoId == "m1" }.state)
        assertEquals(conn("m2", "z", ConnState.ACTIVE, lastSeen = 1L), out.first { it.magoId == "m2" })
    }

    @Test fun aSessionTheServerNoLongerShowsIsMarkedAsLostButAPendingRequestIsKept() {
        val list = listOf(conn("m1", "a", ConnState.ACTIVE), conn("m1", "b", ConnState.PENDING))
        val out = HelperRegistry.applySessions(list, "m1", emptyList(), listOf(pending("b")), now)
        assertEquals(ConnState.REMOVED, out[0].state)
        assertEquals("lost", out[0].reason)
        assertEquals(ConnState.PENDING, out[1].state) // sigue esperando al mago
    }

    @Test fun aSessionTheHelperDidNotKnowIsAddedSoTheListIsTheTruth() {
        val list = listOf(conn("m1", "a", ConnState.PAUSED))
        val out = HelperRegistry.applySessions(list, "m1", listOf(session("a"), session("n", "Nueva", SessionStatus.ACTIVE)), emptyList(), now)
        assertEquals(listOf("a", "n"), out.map { it.sessionId })
        assertEquals(ConnState.ACTIVE, out[1].state)
        assertEquals("Nueva", out[1].name)
    }

    @Test fun anAlreadyRemovedConnectionKeepsItsReasonWhenTheServerStillDoesNotShowIt() {
        val list = listOf(conn("m1", "a", ConnState.REMOVED, reason = "deleted", lastSeen = 1L))
        val out = HelperRegistry.applySessions(list, "m1", emptyList(), emptyList(), now)
        assertEquals(list, out)
    }

    @Test fun expiredAndRevokedStatusesMapToTheirStates() {
        val list = listOf(conn("m1", "a", ConnState.ACTIVE), conn("m1", "b", ConnState.ACTIVE))
        val out = HelperRegistry.applySessions(list, "m1", listOf(session("a", status = SessionStatus.EXPIRED), session("b", status = SessionStatus.REVOKED)), emptyList(), now)
        assertEquals(ConnState.EXPIRED, out[0].state)
        assertEquals(ConnState.REMOVED, out[1].state)
        assertEquals("revoked", out[1].reason)
    }

    @Test fun aReactivatedSessionClearsTheRemovedReason() {
        val list = listOf(conn("m1", "a", ConnState.REMOVED, reason = "lost"))
        val out = HelperRegistry.applySessions(list, "m1", listOf(session("a", status = SessionStatus.ACTIVE)), emptyList(), now)
        assertEquals(ConnState.ACTIVE, out[0].state)
        assertNull(out[0].reason)
    }

    // ---- Bajas ----

    @Test fun leavingRemovesTheConnectionFromTheList() {
        val list = listOf(conn("m1", "a", ConnState.ACTIVE), conn("m1", "b", ConnState.PAUSED))
        assertEquals(listOf("b"), HelperRegistry.removedBy(list, "m1", "a", "left").map { it.sessionId })
    }

    @Test fun whenTheMagoRemovesYouTheConnectionStaysMarkedWithTheReason() {
        val list = listOf(conn("m1", "a", ConnState.ACTIVE), conn("m2", "a", ConnState.ACTIVE))
        for (reason in listOf("deleted", "revoked", "replaced")) {
            val out = HelperRegistry.removedBy(list, "m1", "a", reason)
            assertEquals(ConnState.REMOVED, out[0].state)
            assertEquals(reason, out[0].reason)
            assertEquals(ConnState.ACTIVE, out[1].state) // la «a» del otro mago no se toca
        }
    }

    @Test fun aRejectedRequestIsMarkedOnlyIfItWasPending() {
        val list = listOf(conn("m1", "a", ConnState.PENDING), conn("m1", "b", ConnState.PAUSED), conn("m1", "c", ConnState.PENDING))
        val bySession = HelperRegistry.rejected(list, "m1", "a")
        assertEquals(listOf(ConnState.REMOVED, ConnState.PAUSED, ConnState.PENDING), bySession.map { it.state })
        assertEquals("rejected", bySession[0].reason)
        // Sin id de sesión, todas las pendientes de ese mago
        val all = HelperRegistry.rejected(list, "m1", null)
        assertEquals(listOf(ConnState.REMOVED, ConnState.PAUSED, ConnState.REMOVED), all.map { it.state })
    }

    @Test fun whenTheServerNoLongerRecognisesYouAllTheConnectionsOfThatMagoAreLost() {
        val list = listOf(
            conn("m1", "a", ConnState.ACTIVE), conn("m1", "b", ConnState.REMOVED, reason = "deleted"),
            conn("m2", "c", ConnState.ACTIVE),
        )
        val out = HelperRegistry.authLost(list, "m1")
        assertEquals(ConnState.REMOVED, out[0].state)
        assertEquals("lost", out[0].reason)
        assertEquals("deleted", out[1].reason) // la que ya estaba retirada conserva su motivo
        assertEquals(ConnState.ACTIVE, out[2].state)
    }

    @Test fun removeOnlyDropsTheExactConnection() {
        val list = listOf(conn("m1", "a", ConnState.ACTIVE), conn("m2", "a", ConnState.ACTIVE))
        assertEquals(listOf("m2/a"), HelperRegistry.remove(list, "m1", "a").map { it.key })
    }

    // ---- Presentación ----

    @Test fun theListIsSortedActiveFirstThenPausedPendingExpiredAndRemoved() {
        val list = listOf(
            conn("m", "1", ConnState.REMOVED, "Z"), conn("m", "2", ConnState.EXPIRED, "Y"), conn("m", "3", ConnState.PENDING, "X"),
            conn("m", "4", ConnState.PAUSED, "b"), conn("m", "5", ConnState.ACTIVE, "z"), conn("m", "6", ConnState.PAUSED, "A"),
        )
        assertEquals(listOf("5", "6", "4", "3", "2", "1"), HelperRegistry.sorted(list).map { it.sessionId })
    }

    @Test fun everythingButAlreadyRemovedConnectionsIsTriedOnTheServer() {
        assertTrue(HelperRegistry.canLeaveOnServer(conn("m", "a", ConnState.ACTIVE)))
        assertTrue(HelperRegistry.canLeaveOnServer(conn("m", "a", ConnState.PAUSED)))
        assertTrue(HelperRegistry.canLeaveOnServer(conn("m", "a", ConnState.EXPIRED)))
        // Una pendiente también: lo que dice la lista es lo último que se vio y el mago puede haberla aceptado ya
        assertTrue(HelperRegistry.canLeaveOnServer(conn("m", "a", ConnState.PENDING)))
        assertFalse(HelperRegistry.canLeaveOnServer(conn("m", "a", ConnState.REMOVED)))
    }

    @Test fun theCardButtonSaysSalirForAcceptedSessionsAndQuitarOtherwise() {
        for (state in listOf(ConnState.ACTIVE, ConnState.PAUSED, ConnState.EXPIRED)) assertEquals("Salir", HelperRegistry.leaveLabel(conn("m", "a", state)))
        for (state in listOf(ConnState.PENDING, ConnState.REMOVED)) assertEquals("Quitar", HelperRegistry.leaveLabel(conn("m", "a", state)))
    }

    @Test fun everyStateAndRemovalReasonHasItsOwnText() {
        val texts = ConnState.entries.filter { it != ConnState.REMOVED }.map { HelperRegistry.statusText(conn("m", "a", it)) }
        assertEquals(texts.size, texts.toSet().size)
        val reasons = listOf("rejected", "deleted", "revoked", "replaced", "lost").map { HelperRegistry.statusText(conn("m", "a", ConnState.REMOVED, reason = it)) }
        assertEquals(reasons.size, reasons.toSet().size)
        assertNotNull(reasons.firstOrNull { "borró" in it })
    }

    // ---- Reglas de la plantilla ----

    @Test fun anActiveSessionSendsAndTheButtonsSayEnviarAndSoltar() {
        val first = planActions(SessionStatus.ACTIVE, connected = true, hasPlan = false, dirty = false)
        assertTrue(first.canSend)
        assertFalse(first.canRelease)
        assertEquals("Enviar", first.sendLabel)
        assertEquals("Soltar el control", first.releaseLabel)
        assertNull(first.note)
        // Con plan, solo se envía si hay cambios; y se puede soltar
        val clean = planActions(SessionStatus.ACTIVE, connected = true, hasPlan = true, dirty = false)
        assertFalse(clean.canSend)
        assertTrue(clean.canRelease)
        assertTrue(planActions(SessionStatus.ACTIVE, connected = true, hasPlan = true, dirty = true).canSend)
    }

    @Test fun aPausedSessionLetsYouPrepareTheTemplateWithItsOwnLabels() {
        val a = planActions(SessionStatus.PAUSED, connected = true, hasPlan = false, dirty = false)
        assertTrue(a.canSend)
        assertEquals("Guardar plantilla", a.sendLabel)
        assertEquals("Vaciar plantilla", a.releaseLabel)
        assertNull(a.note)
        assertTrue(planActions(SessionStatus.PAUSED, connected = true, hasPlan = true, dirty = false).canRelease)
    }

    @Test fun anExpiredOrRevokedSessionAdmitsNothingAndSaysWhy() {
        for (status in listOf(SessionStatus.EXPIRED, SessionStatus.REVOKED)) {
            val a = planActions(status, connected = true, hasPlan = true, dirty = true)
            assertFalse(a.canSend)
            assertFalse(a.canRelease)
            assertNotNull(a.note)
        }
        assertTrue("caducado" in planActions(SessionStatus.EXPIRED, true, false, false).note!!)
    }

    @Test fun withoutConnectionNothingCanBeSentWhateverTheStatus() {
        for (status in SessionStatus.entries) {
            val a = planActions(status, connected = false, hasPlan = true, dirty = true)
            assertFalse(a.canSend)
            assertFalse(a.canRelease)
            assertEquals("Hace falta conexión para guardar cambios.", a.note)
        }
    }

    private class MemoryStore : KeyValueStore {
        val m = HashMap<String, Any>()
        override fun getString(key: String) = m[key] as String?
        override fun getBoolean(key: String, default: Boolean) = m[key] as Boolean? ?: default
        override fun getLong(key: String, default: Long) = m[key] as Long? ?: default
        override fun putString(key: String, value: String) { m[key] = value }
        override fun putBoolean(key: String, value: Boolean) { m[key] = value }
        override fun putLong(key: String, value: Long) { m[key] = value }
        override fun remove(key: String) { m.remove(key) }
    }
}
