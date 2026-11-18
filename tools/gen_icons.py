"""
Genera, a partir del kit de data/icons/PNG, todo lo necesario para el cambio de icono y de nombre de la app:

  Android  app/src/main/res/drawable-nodpi/icon_<id>.png        icono original
           app/src/main/res/mipmap-anydpi-v26/ic_<id>.xml        icono adaptable (fondo blanco + icono reducido)
           app/src/main/AndroidManifest.xml                      un <activity-alias> por icono x nombre
  iOS      iosApp/iosApp/Assets.xcassets/AppIcon-<id>.appiconset iconos alternativos (1024 px, sin transparencia)
           iosApp/iosApp/Assets.xcassets/AppIcon.appiconset      icono principal = el icono por defecto
  Común    shared/src/commonMain/kotlin/com/chronix/ui/AppIconCatalog.kt   catálogo + miniaturas en base64

Uso:  python tools/gen_icons.py   (desde la raíz del proyecto). Para añadir un icono, ponlo en ICONS y vuelve a ejecutarlo.
"""
import base64, io, os, shutil, sys
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "data", "icons", "PNG")

# id, nombre visible, archivo del kit
ICONS = [
    ("google", "Google / Android", "Google-Android-Reloj.png"),
    ("apple_ios", "Apple · iOS", "Apple-iOS-Reloj.png"),
    ("samsung", "Samsung · One UI", "Samsung-OneUI-Reloj.png"),
    ("xiaomi", "Xiaomi · MIUI / HyperOS", "Xiaomi-MIUI-HyperOS-Reloj.png"),
    ("oppo", "OPPO / Realme · ColorOS", "OPPO-Realme-ColorOS-Reloj.png"),
    ("oneplus", "OnePlus · OxygenOS", "OnePlus-OxygenOS-Reloj.png"),
    ("huawei", "Huawei · EMUI", "Huawei-EMUI-Reloj.png"),
    ("honor", "Honor · MagicOS", "Honor-MagicOS-Reloj.png"),
    ("vivo", "vivo · Reloj", "vivo-Reloj.png"),
    ("asus", "ASUS · ZenUI", "ASUS-ZenUI-Reloj.png"),
    ("sony_classic", "Sony · Xperia clásico", "Sony-Xperia-Reloj-clasico.png"),
    ("htc", "HTC · Sense", "HTC-Sense-Reloj.png"),
    ("meizu", "Meizu · Flyme", "Meizu-Flyme-Reloj.png"),
    ("transsion", "Transsion · Tecno / Infinix", "Transsion-Tecno-Infinix-Reloj.png"),
    ("google_old", "Google / Android · anterior", "Google-Android-Reloj-anterior.png"),
    ("samsung_old", "Samsung · diseño anterior", "Samsung-OneUI-Reloj-anterior.png"),
    ("oneplus_old", "OnePlus · diseño anterior", "OnePlus-OxygenOS-Reloj-anterior.png"),
    ("sony_variant", "Sony · Xperia, variante", "Sony-Xperia-Reloj-variante.png"),
    ("aosp_kitkat", "Android AOSP · KitKat", "Android-AOSP-KitKat-Reloj.png"),
    ("aosp_gingerbread", "Android AOSP · Gingerbread", "Android-AOSP-Gingerbread-Reloj.png"),
]
DEFAULT_ICON = "google"

# id, nombre que ve el usuario bajo el icono (el primero es el de por defecto)
NAMES = [("chronix", "Chronix"), ("reloj", "Reloj"), ("clock", "Clock"), ("cronometro", "Cronómetro")]
DEFAULT_NAME = "chronix"


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


def main():
    res = os.path.join(ROOT, "app", "src", "main", "res")
    xc = os.path.join(ROOT, "iosApp", "iosApp", "Assets.xcassets")
    kt_items = []

    write(os.path.join(res, "values", "ic_plate.xml"),
          '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_plate">#FFFFFFFF</color>\n</resources>\n')

    for icon_id, label, fname in ICONS:
        im = Image.open(os.path.join(SRC, fname)).convert("RGBA")

        # --- Android: PNG original + icono adaptable (el icono ocupa el 66 % central, sobre fondo blanco)
        os.makedirs(os.path.join(res, "drawable-nodpi"), exist_ok=True)
        im.save(os.path.join(res, "drawable-nodpi", f"icon_{icon_id}.png"), optimize=True)
        write(os.path.join(res, "mipmap-anydpi-v26", f"ic_{icon_id}.xml"),
              '<?xml version="1.0" encoding="utf-8"?>\n'
              '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
              '    <background android:drawable="@color/ic_plate" />\n'
              '    <foreground>\n'
              f'        <inset android:drawable="@drawable/icon_{icon_id}" android:inset="17%" />\n'
              '    </foreground>\n'
              '</adaptive-icon>\n')

        # --- iOS: 1024 px sin transparencia (iOS exige icono opaco); el principal es el icono por defecto
        big = im.resize((1024, 1024), Image.LANCZOS)
        flat = Image.new("RGB", big.size, (255, 255, 255))
        flat.paste(big, mask=big.getchannel("A"))
        if icon_id == DEFAULT_ICON:
            flat.save(os.path.join(xc, "AppIcon.appiconset", "AppIcon-1024.png"), optimize=True)
        else:
            d = os.path.join(xc, f"AppIcon-{icon_id}.appiconset")
            os.makedirs(d, exist_ok=True)
            flat.save(os.path.join(d, "icon.png"), optimize=True)
            write(os.path.join(d, "Contents.json"),
                  '{\n  "images": [\n    {\n      "filename": "icon.png",\n      "idiom": "universal",\n'
                  '      "platform": "ios",\n      "size": "1024x1024"\n    }\n  ],\n'
                  '  "info": {\n    "author": "xcode",\n    "version": 1\n  }\n}\n')

        # --- Miniatura para la lista de ajustes (96 px, base64)
        thumb = im.resize((96, 96), Image.LANCZOS)
        buf = io.BytesIO()
        thumb.save(buf, format="PNG", optimize=True)
        kt_items.append((icon_id, label, base64.b64encode(buf.getvalue()).decode("ascii")))

    # --- Manifiesto: MainActivity sin LAUNCHER y un alias por icono x nombre
    aliases = []
    for icon_id, _, _ in ICONS:
        for name_id, name_label in NAMES:
            enabled = "true" if (icon_id == DEFAULT_ICON and name_id == DEFAULT_NAME) else "false"
            aliases.append(
                f'        <activity-alias\n'
                f'            android:name=".Alias_{icon_id}_{name_id}"\n'
                f'            android:targetActivity=".MainActivity"\n'
                f'            android:enabled="{enabled}"\n'
                f'            android:exported="true"\n'
                f'            android:icon="@mipmap/ic_{icon_id}"\n'
                f'            android:roundIcon="@mipmap/ic_{icon_id}"\n'
                f'            android:label="{name_label}">\n'
                f'            <intent-filter>\n'
                f'                <action android:name="android.intent.action.MAIN" />\n'
                f'                <category android:name="android.intent.category.LAUNCHER" />\n'
                f'            </intent-filter>\n'
                f'        </activity-alias>\n')
    manifest = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<manifest xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <uses-permission android:name="android.permission.VIBRATE" />\n'
        '    <uses-permission android:name="android.permission.INTERNET" />\n'
        '    <application\n'
        f'        android:icon="@mipmap/ic_{DEFAULT_ICON}"\n'
        f'        android:roundIcon="@mipmap/ic_{DEFAULT_ICON}"\n'
        '        android:label="Chronix"\n'
        '        android:theme="@style/Theme.Chronix">\n'
        '        <!-- La actividad real no tiene icono propio: el icono y el nombre del lanzador los pone el\n'
        '             activity-alias activo (uno solo habilitado). Generado por tools/gen_icons.py -->\n'
        '        <activity\n'
        '            android:name=".MainActivity"\n'
        '            android:exported="false"\n'
        '            android:screenOrientation="portrait"\n'
        '            android:configChanges="orientation|screenSize|keyboardHidden|smallestScreenSize|screenLayout|uiMode" />\n'
        + "".join(aliases) +
        '    </application>\n'
        '</manifest>\n')
    write(os.path.join(ROOT, "app", "src", "main", "AndroidManifest.xml"), manifest)

    # --- Catálogo Kotlin
    lines = [
        "// GENERADO por tools/gen_icons.py. No editar a mano.\n",
        "package com.chronix.ui\n\n",
        "/** Icono de la app elegible en los ajustes. `thumbB64` es una miniatura PNG de 96 px en base64. */\n",
        "class AppIcon(val id: String, val label: String, val thumbB64: String)\n\n",
        "/** Nombre de la app elegible en los ajustes (solo Android puede cambiarlo). */\n",
        "class AppName(val id: String, val label: String)\n\n",
        "object AppIconCatalog {\n",
        f'    const val DEFAULT_ICON = "{DEFAULT_ICON}"\n',
        f'    const val DEFAULT_NAME = "{DEFAULT_NAME}"\n\n',
        "    val names: List<AppName> = listOf(\n",
    ]
    for n_id, n_label in NAMES:
        lines.append(f'        AppName("{n_id}", "{n_label}"),\n')
    lines.append("    )\n\n    val icons: List<AppIcon> = listOf(\n")
    for icon_id, label, b64 in kt_items:
        lines.append(f'        AppIcon("{icon_id}", "{label}", "{b64}"),\n')
    lines.append("    )\n}\n")
    write(os.path.join(ROOT, "shared", "src", "commonMain", "kotlin", "com", "chronix", "ui", "AppIconCatalog.kt"), "".join(lines))
    print("ok:", len(ICONS), "iconos,", len(ICONS) * len(NAMES), "alias")


main()
