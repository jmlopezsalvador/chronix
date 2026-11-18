@file:OptIn(ExperimentalForeignApi::class)

package com.chronix.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

/** iOS: sin comportamiento especial (la barra de estado y el indicador de inicio se ocultan en ContentView.swift). */
@Composable
actual fun SystemBarsEffect(darkBackground: Boolean) {}

@Composable
actual fun rememberVibrate(): () -> Unit = remember {
    {
        val generator = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
        generator.prepare()
        generator.impactOccurred()
    }
}

@Composable
actual fun rememberSoftVibrate(): () -> Unit = remember {
    {
        val generator = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
        generator.prepare()
        generator.impactOccurred()
    }
}

@Composable
actual fun rememberAppAppearance(): AppAppearance = remember { IosAppearance() }

/**
 * iOS solo permite cambiar el icono (los iconos alternativos están en Assets.xcassets como `AppIcon-<id>`, generados por
 * tools/gen_icons.py), y siempre con su propio aviso del sistema. El nombre no se puede cambiar en ejecución.
 */
private class IosAppearance : AppAppearance {
    override val canChangeName = false
    override val restartsApp = false

    override fun apply(iconId: String, nameId: String) {
        // El icono por defecto es el principal («AppIcon»): se vuelve a él con nil
        val name = if (iconId == AppIconCatalog.DEFAULT_ICON) null else "AppIcon-$iconId"
        IosAppIconBridge.setter?.setIcon(name)
    }
}

/**
 * Kotlin/Native no expone `UIApplication.setAlternateIconName`, así que lo hace Swift (iOSApp.swift): implementa este
 * protocolo y se registra en [IosAppIconBridge] al arrancar.
 */
interface IosIconSetter {
    /** `null` = icono principal; si no, el nombre del icono alternativo (`AppIcon-<id>`). */
    fun setIcon(name: String?)
}

object IosAppIconBridge {
    var setter: IosIconSetter? = null
}

class UserDefaultsStore : KeyValueStore {
    private val d = NSUserDefaults.standardUserDefaults
    override fun getString(key: String): String? = d.stringForKey(key)
    override fun getBoolean(key: String, default: Boolean) =
        if (d.objectForKey(key) == null) default else d.boolForKey(key)
    override fun getLong(key: String, default: Long) =
        if (d.objectForKey(key) == null) default else d.integerForKey(key)
    override fun putString(key: String, value: String) = d.setObject(value, key)
    override fun putBoolean(key: String, value: Boolean) = d.setBool(value, key)
    override fun putLong(key: String, value: Long) = d.setInteger(value, key)
    override fun remove(key: String) = d.removeObjectForKey(key)
}
