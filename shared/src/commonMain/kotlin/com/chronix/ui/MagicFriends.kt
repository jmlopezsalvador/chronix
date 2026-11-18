package com.chronix.ui

import com.magicfriends.client.ConnectionState
import com.magicfriends.client.MagicFriendsClient
import com.magicfriends.client.Role
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.magicfriends.client.KeyValueStore as MagicFriendsStore

/**
 * Adaptador del almacén de Chronix (SharedPreferences / UserDefaults) a la interfaz de la librería. La librería guarda
 * aquí la identidad anónima del dispositivo y la última instantánea remota; sus claves llevan el prefijo `mf.`, así que
 * no chocan con los ajustes de Chronix.
 */
internal class MagicFriendsPrefs(private val prefs: KeyValueStore) : MagicFriendsStore {
    override fun getString(key: String): String? = prefs.getString(key)
    override fun putString(key: String, value: String) = prefs.putString(key, value)
    override fun remove(key: String) = prefs.remove(key)
}

/**
 * Chronix como **mago** de Magic Friends. Solo existe mientras el interruptor «Magic Friends» esté activado (y la
 * compilación lleve la configuración): con él apagado no se crea el cliente, no se abre la red y no se genera identidad.
 *
 * El ajuste efectivo se calcula **en el último instante** con [remoteOverrides] (el cronómetro lo pide en cada vuelta y en
 * cada Parar). Sin red, la librería devuelve la última instantánea guardada: el efecto no depende de la conexión.
 */
internal class MagicFriendsController(prefs: KeyValueStore, private val scope: CoroutineScope) {
    val client = MagicFriendsClient(
        role = Role.MAGO,
        serverUrl = MagicFriendsConfig.SERVER_URL,
        store = MagicFriendsPrefs(prefs),
        scope = scope,
        appKey = MagicFriendsConfig.APP_KEY.ifBlank { null },
    )

    private var watchdog: Job? = null

    /** Abre la conexión (la librería reintenta sola) y vigila que no se quede esperando demasiado tras una caída larga. */
    fun start() {
        client.start()
        watchdog?.cancel()
        watchdog = scope.launch {
            // La espera de reconexión de la librería crece hasta 30 s. Si llevamos varios segundos caídos, se reinicia el
            // cliente para volver a intentarlo ya (tras volver la cobertura, o al recuperar la app del segundo plano).
            client.connection.collectLatest { state ->
                if (state is ConnectionState.Disconnected) {
                    delay(RECONNECT_WATCHDOG_MS)
                    client.stop()
                    client.start()
                }
            }
        }
    }

    fun stop() {
        watchdog?.cancel()
        watchdog = null
        client.stop()
    }

    /**
     * Reconecta para que el servidor devuelva la lista de sesiones actual (y la librería reconcilie la instantánea remota).
     * Hace falta tras **borrar** una sesión: el servidor solo envía un `ack` al mago y la librería (0.1.0) no la retira de
     * `client.sessions` ni deja de aplicar sus ajustes hasta reconectar. Si la librería lo corrige, esto sobra pero no estorba.
     */
    fun refresh() {
        client.stop()
        client.start()
    }

    /** Ajustes remotos validados, **ahora mismo** (vacíos si no hay sesión activa o ha caducado). */
    fun remoteOverrides(): RemoteOverrides = parseRemoteOverrides(client.currentRemoteSettings())

    private companion object {
        const val RECONNECT_WATCHDOG_MS = 8_000L
    }
}
