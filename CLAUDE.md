# Chronix (Android + iOS)

App móvil para **Android y iOS** con un único código en Kotlin Multiplatform + Compose Multiplatform.
Paquete `com.chronix`. Repo: https://github.com/jmlopezsalvador/chronix.git
El usuario habla español: responde y comenta el código en español.

## Mapa de carpetas
| Carpeta | Para qué sirve |
|---|---|
| `shared/` | Módulo KMP con TODO el código: lógica pura, interfaz Compose, ajustes. `commonMain` (común), `androidMain` / `iosMain` (solo lo del sistema, `expect`/`actual`), `commonTest` (tests). |
| `app/` | Solo la envoltura Android (`MainActivity`, manifiesto, recursos, icono). |
| `iosApp/` | Envoltura iOS en SwiftUI + `project.yml` (XcodeGen genera el `.xcodeproj` en CI; no se versiona). |
| `.github/workflows/ios.yml` | Compila en un Mac de GitHub y publica un `.ipa` sin firmar. Solo `workflow_dispatch`. |
| `docs/` | `ARQUITECTURA.md` (base técnica, fuente de verdad de versiones y reglas) y `SPEC.md` (especificación funcional, se crea al inicializar). |
| `data/` | Material de referencia que aporta el usuario (capturas, icono, recursos). Se puede leer. |
| `data/android_build/` | APK descargados/generados. No se versionan. |
| `data/ios_build/` | `.ipa` descargado de Actions. No se versiona. |
| `../magicfriends/` | Repo hermano (`C:\Users\jmlop\magicfriends`): servicio + librería cliente KMP de Magic Friends. Chronix lo consume con `includeBuild("../magicfriends/client")`. **No modificarlo desde esta sesión**: si la librería necesita un cambio, describírselo al usuario (se hace en su propia sesión). |
| `ignore/` | **Ignorar por completo** (ver reglas). |

## Reglas para Claude
- **Ignorar por completo la carpeta `ignore/`**: no leerla, no listarla, no buscar dentro, no usarla como referencia. Excluirla
  también de búsquedas (Glob/Grep) y del control de versiones. Es el cajón del usuario para material suelto.
- La base técnica es **`docs/ARQUITECTURA.md`** (versiones, estructura, Gradle, CI de iOS, reglas de código). Si algo choca con él, consultar al usuario.
- **Antes de lanzar el workflow de iOS en GitHub Actions, preguntar siempre al usuario** (cuesta cuota de macOS; se acumulan mejoras).
  Por defecto se lanza **sin tests** (el usuario prueba en su iPhone); solo con tests si él lo pide.
- No inventar funcionalidad: la fuente de verdad es `docs/SPEC.md` y lo que pida el usuario. Ante la duda, preguntar.
- **No hacer commit ni push sin que el usuario lo pida.** No commitear si la compilación falla.
- Datos solo en local (SharedPreferences / UserDefaults y ficheros internos). Sin cuentas. **La red solo con Magic Friends activado** (SPEC §9); apagado, la app no abre ninguna conexión.
- **El repo es público: la URL del servidor y la `appKey` de Magic Friends NUNCA van en código, docs ni ficheros versionados** (ver «Configuración de Magic Friends» abajo). No pegar la `appKey` en el chat.
- Mantener este `CLAUDE.md` al día: al cambiar de forma relevante la app, el flujo o las decisiones, actualizarlo en la misma tarea.

## Qué hace la app
Herramienta para un **efecto de magia**: emula la app de reloj de un fabricante (Alarma, Reloj mundial, Cronómetro,
Temporizador). En la fase 1 solo el **cronómetro** es funcional y realista; las otras pestañas no hacen nada.
- 7 *look and feel* (Samsung, iPhone, Pixel, OnePlus, OPPO, Xiaomi, Huawei; referencias en `data/Visuales/` y `data/kit/`), elegibles en ajustes secretos.
- **Ajustes secretos:** pulsación larga (1,5 s configurable) en cualquier zona vacía de la pantalla (no sobre controles; ver
  `secretAccess`/`blocksSecret` en `ui/Gestures.kt`). Incluyen icono y nombre de la app (modal de confirmación; en Android
  reinicia), guía de inicio, pantalla de Instrucciones y créditos.
- **Modo forzaje:** pulsación larga en *Restablecer* → vibración. Dos modos (`ForcePlan`): *por vueltas* (centésimas del
  **tiempo de vuelta** por defecto, u opción de forzar el total, que se deshabilita si los totales no se ven) y *cronómetro* (un solo valor, en el
  primer Parar tras armar: solo centésimas o contador completo; la vuelta en curso se ajusta sola). Regla: lo que se fuerza se
  sustituye siempre (el contador completo puede retroceder); lo que no se fuerza nunca retrocede. Al terminar el forzaje el
  cronómetro vuelve a ser normal. Nada debe ser visible. Ajuste «Mostrar tiempos totales» para ocultar la columna de totales.
- **Magic Friends (SPEC §9):** un ayudante cambia en remoto, en directo, los ajustes de forzaje (lista blanca `RemoteKeys`). Efectivo = `remoto ?: local`; se lee en cada
  vuelta/Parar (plan vivo en `Stopwatch.resetAndArm { … }`), también tras armar. Interruptor del móvil en los ajustes (apagado = sin red). Sin red usa la última instantánea.
  Código: `ui/RemoteSettings.kt` (lista blanca, validación y parche del ayudante), `ui/MagicFriends.kt` (controladores del mago y del ayudante + almacén), `ui/MagicFriendsScreen.kt` (pantalla del mago),
  `ui/HelperScreen.kt` (pantalla del ayudante) y `ui/ForcingEditor.kt` (editor de forzaje compartido por los ajustes locales y el ayudante, con `ForcingValues`).
  **Fases 1 y 2 hechas** (Chronix como mago y como ayudante). El ayudante manda siempre el **plan completo** (seis claves) con el botón «Enviar» (nunca por tecla) y puede «Soltar el control»; su conexión solo existe con su pantalla abierta (SPEC R40-R45).
  Fase 3 pendiente: QR y cambiar la caducidad de una sesión.
- Detalle y decisiones en `docs/SPEC.md` (fuente de verdad funcional).
- **Código:** lógica pura en `shared/.../domain/Stopwatch.kt` (reloj inyectado, con tests); temas en `ui/ClockTheme.kt`;
  un composable por diseño en `ui/<Marca>Style.kt` (despacha `ui/StopwatchScreen.kt`); piezas comunes en `ui/Common.kt`,
  `Icons.kt`, `Dials.kt`, `Widgets.kt`. Para otro fabricante: nuevo `ClockTheme` + `ClockStyle` + su `<Marca>Style.kt`.
- **Iconos:** 20 iconos de `data/icons`; `python tools/gen_icons.py` regenera alias de Android (`AndroidManifest.xml`), iconos de iOS
  (`Assets.xcassets/AppIcon-<id>`) y `ui/AppIconCatalog.kt`. Si se añade/cambia un control pulsable, marcarlo con `blocksSecret()`.
- **Estado (fase 1):** cronómetro en 7 diseños + forzaje + ajustes secretos. Probado en el emulador Android (los 7 diseños, forzaje en Samsung y OnePlus); falta probarlo en un móvil real (vibración). iOS sin compilar aún (la háptica `UIImpactFeedbackGenerator` y los gestos son lo más arriesgado).
  Magic Friends (mago) probado en el emulador Android contra el servidor real (conectar, crear/aceptar/activar/borrar sesión, cambio remoto tras armar, sin red con la app reiniciada, interruptor apagado = 0 sockets).
  Magic Friends (ayudante) probado en el emulador con el CLI de `magicfriends` de mago (unirse, aceptar, activar, enviar el plan completo, soltar el control, pausa, revocación, emparejamiento tras reiniciar). Falta una prueba app↔app con dos móviles.
  iOS: lo nuevo sin compilar (Ktor Darwin de la librería `iosMain`, `rememberSoftVibrate`, `remove` de `UserDefaultsStore`); el primer Actions con la librería puede fallar en `iosMain` de la librería.

## Reglas de código para que compile en iOS
- En `commonMain`: nada de `java.*` ni `android.*`. Usar `kotlinx-datetime` (0.6.1), `kotlin.math.*` y, si hace falta aritmética
  grande, `bignum` (`com.ionspin.kotlin:bignum:0.3.10`). Sin `String.format`, `Locale`, `SimpleDateFormat`, `Thread`, `synchronized`.
- Sin recursos de Android (`R.string`) en común; textos en el código.
- Iconos dibujados con `Canvas`/`Path` de Compose (no `material-icons-extended`).
- Lógica de dominio pura y sin UI, con tests en `commonTest` (y reloj inyectable si depende del tiempo).
- Lo del sistema (sensores, vibración, galería, portapapeles…): `expect` en `commonMain/.../ui/Platform.kt` + `actual` en `androidMain`/`iosMain`.
- `Platform.ios.kt` lleva `@file:OptIn(ExperimentalForeignApi::class)` (cinterop de Kotlin/Native).
- El fallo de iOS solo se ve en Actions (~20 min): revisar con cuidado antes de lanzarlo.
- Con scripts: en esta máquina los heredocs de Bash con contenido complejo fallan; escribe los `.py` con la herramienta Write y ejecútalos.

## Compilar y probar (Windows)
- Kotlin 2.0.21, Compose Multiplatform 1.7.1, AGP 8.7.3, Gradle 8.9, JDK 17, minSdk 26, target/compileSdk 35, iOS mínimo 15.0.
- `JAVA_HOME` apunta al JDK 17: `C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot`.
- SDK de Android en `C:\Users\jmlop\AppData\Local\Android\Sdk`: crear `local.properties` con `sdk.dir=...` (ignorado por git). Ahí van también `magicfriends.serverUrl` y `magicfriends.appKey` (ver abajo).
- **Configuración de Magic Friends** (el repo es público, estos valores no se versionan): la URL del servidor y la `appKey` se leen de `-Pmagicfriends.serverUrl` / `-Pmagicfriends.appKey`, de las variables
  `MAGICFRIENDS_SERVER_URL` / `MAGICFRIENDS_APP_KEY` o de `local.properties`, y `shared/build.gradle.kts` genera `MagicFriendsConfig.kt` en `build/`. La `appKey` está en `~/.magicfriends-cli/app-key.txt` (no pegarla en el chat).
  Sin valores la app compila con Magic Friends desactivado. En Actions salen de los secretos del repo `MAGICFRIENDS_SERVER_URL` y `MAGICFRIENDS_APP_KEY` (creados con `gh secret set`).
- Hace falta el repo hermano en `C:\Users\jmlop\magicfriends` (`includeBuild("../magicfriends/client")`). En CI, `ios.yml` lo clona en `../magicfriends`; si pasa a privado, necesitará un token de solo lectura.
- Probar contra el servidor real con el CLI del repo hermano (`node tools/cli/mf.ts ayudante --url <url>`, interactivo; para scriptearlo, un pequeño controlador en Node con `spawn` y esperas entre comandos; PowerShell 5.1 no sirve para canalizarle la entrada).
- `./gradlew.bat :shared:testDebugUnitTest assembleDebug` → APK en `app/build/outputs/apk/debug/app-debug.apk`.
  Copiar el APK a `data/android_build/` si se quiere conservar.
- Emulador: el AVD `calcu` ya no existe. Usar **`chronix-emu`** (Android 15, pixel_6, creado con `avdmanager`; `emulator -avd chronix-emu -port 5556`) y **no usar `omniweb-emu` ni `calculadora-emu`**: son de otras sesiones de Claude
  (mirar con `adb devices` y qué app tiene el foco antes de pulsar nada). Poner la zona horaria `Europe/Madrid` (arranca en GMT). Gboard puede salir en negro o abrir un tutorial de lápiz táctil (es del emulador; `adb shell settings put secure stylus_handwriting_enabled 0`).
- `gh` en Windows: `C:\Program Files\GitHub CLI\gh.exe` (hace falta `gh auth login` una vez).
- En Windows los destinos de iOS se ignoran (`kotlin.native.ignoreDisabledTargets=true`); Android compila y testea igual.

## Flujo de trabajo
1. Desarrollar y probar en Android/JVM en local (emulador).
2. Commit y push cuando el usuario lo pida.
3. **Preguntar antes de lanzar Actions** y acumular mejoras: `gh workflow run ios.yml --ref main` (opción `-f tests=true` solo si se pide).
4. Descargar el artefacto a `data/ios_build/`: `gh run download <id> -n <Nombre>-ios-sin-firmar -D data/ios_build`. **Después borrar el run** (`gh run delete <id>`): el `.ipa` lleva dentro la `appKey` y el repo es público (el artefacto ya caduca a 1 día). Al pasar los repos a privado, rotar la `appKey` (`wrangler secret put APP_SECRET` en el repo hermano + secreto de Actions + `local.properties`).
5. Instalar el `.ipa` con Sideloadly/AltStore (Apple ID gratis; caduca a los 7 días; activar Modo desarrollador en el iPhone).

## Lecciones de iOS ya aprendidas
- `gradlew` debe estar en git con permiso de ejecución (`git update-index --chmod=+x gradlew`).
- En iOS, `UIDevice.proximityState` se lee por sondeo; apagar y reactivar la monitorización deja el estado pegado. Mantenerla activa.
- Barra de estado e indicador de inicio se ocultan desde SwiftUI (`ContentView.swift`); el bloqueo automático con `idleTimerDisabled`.

## Instrucciones para el usuario
La app tiene una pantalla de «Instrucciones» (`ui/InstructionsScreen.kt`) y una guía de inicio (`ui/GuideDialog.kt`). Mantenerlas
al día en la misma tarea en que se cambie una función o un ajuste.
