package com.chronix.ui

import android.app.Activity
import android.content.Context
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

@Composable
actual fun rememberVibrate(): () -> Unit {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.vibrate(VibrationEffect.createOneShot(120L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
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
}
