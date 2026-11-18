package com.chronix.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Android: sin comportamiento especial (aquí iría el modo inmersivo, pantalla siempre encendida, etc.). */
@Composable
actual fun SystemBarsEffect(darkBackground: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkBackground
            controller.isAppearanceLightNavigationBars = !darkBackground
        }
    }
}

private fun vibratorOf(context: Context): Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
    (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
} else {
    @Suppress("DEPRECATION")
    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
}

@Composable
actual fun rememberVibrate(): () -> Unit {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        {
            vibratorOf(context)?.vibrate(VibrationEffect.createOneShot(120L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}

@Composable
actual fun rememberSoftVibrate(): () -> Unit {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        {
            // Pulso corto y de poca amplitud (en móviles sin control de amplitud sigue siendo breve)
            vibratorOf(context)?.vibrate(VibrationEffect.createOneShot(40L, 60))
        }
    }
}

@Composable
actual fun rememberAppAppearance(): AppAppearance {
    val context = LocalContext.current
    return remember(context) { AndroidAppearance(context) }
}

/**
 * Un `activity-alias` por icono x nombre (ver AndroidManifest.xml, generado por tools/gen_icons.py). Solo uno está
 * habilitado a la vez. Al cambiarlo se relanza la app, que es lo que hace falta para que el lanzador lo refresque.
 */
private class AndroidAppearance(private val context: Context) : AppAppearance {
    override val canChangeName = true
    override val restartsApp = true

    override fun apply(iconId: String, nameId: String) {
        val pm = context.packageManager
        val pkg = context.packageName
        fun component(i: String, n: String) = ComponentName(pkg, "$pkg.Alias_${i}_$n")
        val target = component(iconId, nameId)
        // Primero se habilita el nuevo y luego se deshabilitan los demás, para no quedarse sin icono en el lanzador
        pm.setComponentEnabledSetting(target, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        for (icon in AppIconCatalog.icons) for (name in AppIconCatalog.names) {
            val c = component(icon.id, name.id)
            if (c != target) pm.setComponentEnabledSetting(c, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
        // Reinicio: se cierra la tarea y se vuelve a abrir por el alias que quedó habilitado
        val launch = pm.getLaunchIntentForPackage(pkg)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        var ctx = context
        while (ctx is ContextWrapper && ctx !is Activity) ctx = ctx.baseContext
        (ctx as? Activity)?.finishAffinity()
        if (launch != null) context.startActivity(launch)
    }
}

class SharedPrefsStore(context: Context) : KeyValueStore {
    private val p = context.getSharedPreferences("chronix", Context.MODE_PRIVATE)
    override fun getString(key: String): String? = p.getString(key, null)
    override fun getBoolean(key: String, default: Boolean) = p.getBoolean(key, default)
    override fun getLong(key: String, default: Long) = p.getLong(key, default)
    override fun putString(key: String, value: String) = p.edit().putString(key, value).apply()
    override fun putBoolean(key: String, value: Boolean) = p.edit().putBoolean(key, value).apply()
    override fun putLong(key: String, value: Long) = p.edit().putLong(key, value).apply()
    override fun remove(key: String) = p.edit().remove(key).apply()
}
