package com.chronix

import androidx.compose.ui.window.ComposeUIViewController
import com.chronix.ui.ChronixRoot
import com.chronix.ui.SettingsStore
import com.chronix.ui.UserDefaultsStore
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    ChronixRoot(SettingsStore(UserDefaultsStore()))
}
