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
        watchdog = scope.watchReconnection(client)
    }

    fun stop() {
        watchdog?.cancel()
        watchdog = null
        client.stop()
    }

    /** Ajustes remotos validados, **ahora mismo** (vacíos si no hay sesión activa o ha caducado). */
    fun remoteOverrides(): RemoteOverrides = parseRemoteOverrides(client.currentRemoteSettings())
}

/**
 * Chronix como **ayudante** de Magic Friends: controla en remoto los ajustes de forzaje de otro mago. Hay **un cliente por mago**
 * (la librería 0.2 lo pide así), todos con el mismo almacén, y por eso con la misma identidad (`mf.helper.*`), distinta de la del
 * mago. No necesita la `appKey`: solo el mago la usa, para registrarse.
 *
 * - Con [magoId]: cliente conectado a ese mago (para trabajar con una de sus sesiones). Solo existe mientras la pantalla de esa
 *   conexión está abierta (sin pantalla no hay red).
 * - Sin [magoId]: cliente que no conecta y solo sirve para canjear un código ([MagicFriendsClient.join]).
 */
internal class MagicFriendsHelper(prefs: KeyValueStore, private val scope: CoroutineScope, magoId: String? = null) {
    val client = MagicFriendsClient(
        role = Role.HELPER,
        serverUrl = MagicFriendsConfig.SERVER_URL,
        store = MagicFriendsPrefs(prefs),
        scope = scope,
        magoId = magoId,
    )

    private var watchdog: Job? = null

    /** Conecta con el mago indicado (la librería reintenta sola). Sin mago no hace nada. */
    fun start() {
        client.start()
        watchdog?.cancel()
        watchdog = scope.watchReconnection(client)
    }

    fun stop() {
        watchdog?.cancel()
        watchdog = null
        client.stop()
    }
}

/**
 * La espera de reconexión de la librería crece hasta 30 s. Si el cliente lleva [RECONNECT_WATCHDOG_MS] caído, se reinicia
 * para volver a intentarlo ya (al volver la cobertura o al recuperar la app del segundo plano).
 */
private fun CoroutineScope.watchReconnection(client: MagicFriendsClient): Job = launch {
    client.connection.collectLatest { state ->
        if (state is ConnectionState.Disconnected) {
            delay(RECONNECT_WATCHDOG_MS)
            client.stop()
            client.start()
        }
    }
}

private const val RECONNECT_WATCHDOG_MS = 8_000L
