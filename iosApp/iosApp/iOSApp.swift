import SwiftUI
import Shared

/// Cambia el icono de la app. Kotlin/Native no expone `setAlternateIconName`, así que se hace aquí y se registra en
/// `IosAppIconBridge` al arrancar. Los iconos alternativos son los `AppIcon-<id>` del catálogo de assets.
final class IconSetter: NSObject, IosIconSetter {
    func setIcon(name: String?) {
        DispatchQueue.main.async {
            let app = UIApplication.shared
            guard app.supportsAlternateIcons else { return }
            app.setAlternateIconName(name, completionHandler: nil)
        }
    }
}

@main
struct iOSApp: App {
    private let iconSetter = IconSetter()

    init() {
        IosAppIconBridge.shared.setter = iconSetter
    }

    var body: some Scene {
        WindowGroup { ContentView() }
    }
}
