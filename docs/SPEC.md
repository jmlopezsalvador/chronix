# Chronix · Especificación funcional

Fuente de verdad funcional. Los requisitos están numerados; las dudas abiertas, al final.

## 1. Concepto
Herramienta para un **efecto de magia**. Emula una app de reloj real (cronómetro, alarmas, reloj mundial, temporizador)
para que parezca auténtica. Solo el **cronómetro** es realista y funcional en la fase 1. La app permite **forzar las
centésimas** de las vueltas y de la parada a valores preparados de antemano.

- Plataformas: Android + iOS (un único código KMP). Orientación vertical. Idioma: español.
- Todo en local. Sin cuentas. La red solo se usa con **Magic Friends** activado (§9); por defecto está apagado y la app no abre ninguna conexión.

## 2. Estructura de la app
- R1. Al abrir, la app entra directamente en la pestaña **Cronómetro**.
- R2. Barra inferior con 4 pestañas: **Alarma, Reloj mundial, Cronómetro, Temporizador** (orden de la captura de Samsung).
- R3. **Fase 1:** solo funciona el cronómetro. Las otras 3 pestañas pueden tocarse, pero no hacen nada (o muestran una pantalla estática verosímil). Se diseñarán en fases posteriores con cada look and feel.

## 3. Cronómetro (realista)
- R4. Formato `MM:SS,cc` (minutos, segundos, centésimas), como en Samsung. Debe pasar a `H:MM:SS,cc` si supera la hora.
- R5. Estados y botones (textos según el look and feel):
  - Parado en 0: **Vuelta** (desactivado/gris) + **Iniciar**.
  - En marcha: **Vuelta** + **Parar** (rojo).
  - En pausa: **Restablecer** + **Reanudar**.
- R6. **Vuelta:** registra una fila con nº de vuelta, tiempo de vuelta y tiempo total. Lista con la última vuelta arriba.
- R7. Mientras hay vueltas, bajo el tiempo principal se muestra el tiempo de la vuelta en curso (en gris), como en Samsung.
- R8. Colores de vueltas (según look and feel): la más corta en un color (azul en Samsung) y la más larga en rojo; el resto, normales. Solo con 2 o más vueltas completadas (por confirmar).
- R9. Restablecer pone todo a cero y borra las vueltas.
- R10. El tiempo se mide con reloj monotónico, no por acumulación de ticks, para que no derive.

## 4. Look and feel (temas)
- R11. Varios aspectos visuales de la misma app, imitando el reloj de cada fabricante. Referencias en `data/Visuales/` y, sobre todo, en `data/kit/Kit_cronometros/<marca>/` (Ficha.md + capturas): **Samsung, iPhone, Pixel, OnePlus, OPPO, Xiaomi y Huawei**.
- R12. **Samsung** (One UI): fondo degradado lila claro, botones circulares (secundario blanco, primario lila con texto azul; Parar en rosa con texto rojo), barra inferior en píldora con iconos, dos puntos de página, menú de tres puntos.
- R13. Los temas se eligen en los ajustes secretos. Cada diseño es un composable propio (`ui/*Style.kt`) con su `ClockTheme` (colores, textos en español, pestañas). Todos comparten la lógica y el gesto de armado, que va en su botón de reinicio (Pixel: siempre visible; el resto: en pausa).
- R13b. iPhone: dos páginas deslizables (digital / analógica). Huawei, OnePlus y OPPO llevan esfera, aro o esfera punteada. Samsung: dos páginas deslizables (`data/Visuales/Samsung 5.jpeg`): analógica (primer punto: corona de marcas, tiempo digital en el centro, aguja azul del total y gris de la vuelta, tabla debajo) y digital (segundo punto, la de inicio).
- R13c. Colores de vueltas por diseño; Xiaomi y Huawei no colorean. Los textos usan coma decimal.
- R14. La arquitectura debe separar la **lógica** del cronómetro de la **presentación**, para añadir temas sin tocar la lógica.

## 5. Ajustes secretos
- R15. **Acceso:** pulsación larga en **cualquier zona vacía de la pantalla**, es decir, que no sea un control (botones, barra de pestañas). Ya no hay botón invisible en la esquina. Duración por defecto **1,5 s**, configurable (0,5 a 5 s). Si el dedo se mueve o se usan dos dedos, se cancela, así que deslizar (pager, listas) no la activa. Funciona con el cronómetro parado, en marcha o en pausa y es igual en todos los diseños. Los controles se marcan con `blocksSecret()`; el detector va en la raíz (`secretAccess`).
- R16. Contenido (fase 1; las cajas de centésimas son de texto numérico normal, se pueden borrar y el teclado se cierra al tocar fuera):
  - Elegir el look and feel.
  - Duración de la pulsación larga de acceso.
  - **Lista de centésimas forzadas** por vuelta (vuelta 1, 2, 3, 4…): dos dígitos cada una.
  - Vibración al armar el forzaje (activar/desactivar, por confirmar).
  - **Magic Friends:** interruptor «Usar Magic Friends» y, con él activo, botón «Sesiones» (§9).

## 6. Modo forzaje (el efecto)
- R17. **Armado:** pulsación larga (~0,8 s) sobre **Restablecer** → el cronómetro se pone a cero, vibra el móvil y queda armado el forzaje. Sin ningún cambio visual.
- R18. Los ajustes guardan una **lista de centésimas** (Vuelta 1: 45, Vuelta 2: 96…). Con el forzaje armado, al pulsar **Vuelta** la *n*-ésima vez, el **tiempo de vuelta** de esa vuelta termina en la centésima configurada para *n*. El **tiempo total** es la consecuencia (suma de las vueltas). *(Definición corregida: el forzaje va siempre en el tiempo de vuelta por defecto, en todos los diseños.)*
- R18b. **Coherencia:** la otra cifra de la vuelta sale por diferencia (si se fuerza el total, vuelta = total − total anterior) o por suma (si se fuerza la vuelta, total = total anterior + vuelta), así la tabla siempre cuadra.
- R18b2. **Opción «Forzar las centésimas en»** (ajustes secretos): *Tiempo de vuelta* (por defecto, recomendado) o *Tiempo total*. Con *Tiempo total*, las centésimas forzadas van en el total de cada vuelta y la vuelta sale por diferencia.
- R18b4. **Aviso:** si está elegido *Tiempo total* y el diseño activo no muestra totales (`ClockTheme.showsTotal = false`, hoy solo iPhone), al elegir esa opción o al cambiar a ese diseño sale un **modal** que lo advierte («Este diseño no muestra el total…»). No cambia el ajuste: solo avisa.
- R18b3. **El ajuste es siempre al alza:** el resultado es el menor tiempo que no baja del real y acaba en las centésimas configuradas. Si las centésimas reales aún no han llegado a las forzadas, se queda en el mismo segundo (1,20 con 50 → 1,50); si ya las pasaron, sube al segundo siguiente (1,60 con 01 → 2,01, nunca 1,01). Nunca se resta ni se salta más de 1 s. Si no supera la vuelta anterior, sube de 1 en 1 segundo.
- R18c. El forzaje solo actúa sobre las **vueltas**. La pausa muestra el tiempo real.
- R19. Cuando se han consumido todos los valores, el forzaje termina: las vueltas siguientes son normales.
- R20. **Restablecer** con pulsación corta pone a cero y desarma el forzaje.
- R21. El estado de armado no se guarda al cerrar la app.
- R22. Nada del forzaje debe ser visible ni detectable: sin textos, sin indicadores.
- R22b. La lista de ajustes viene por defecto con **4** posiciones (valor 00) y se puede ampliar **sin límite**.

## 6a. Modos de forzaje, totales ocultos y modo cronómetro
- R30. **Mostrar tiempos totales** (ajustes, junto al look and feel): interruptor, activo por defecto. Desactivado, cada diseño muestra una versión sin la columna/línea de totales (Samsung, OnePlus, OPPO, Xiaomi, Huawei y Pixel; el iPhone ya no la tiene y el interruptor aparece deshabilitado).
- R31. Con los totales ocultos (por el ajuste o por el diseño) **no se puede forzar el total**: la opción «Tiempo total» aparece deshabilitada y, si estaba elegida, pasa sola a «Tiempo de vuelta» con un modal que lo avisa. Esto sustituye al aviso anterior de «el diseño no muestra el total».
- R32. **Modo de forzaje** (ajustes): *Por vueltas* (R17–R22) o *Cronómetro (al pulsar Parar)*.
- R32b. **Regla general del forzaje:** lo que se fuerza se **sustituye siempre** por el número configurado; lo que no se fuerza **nunca viaja hacia atrás**. En el modo por vueltas se fuerzan las centésimas (los minutos y segundos de la vuelta no se fuerzan, así que van siempre al alza). En el modo cronómetro con *solo centésimas*, igual. En el modo cronómetro con *contador completo* se fuerzan los tres campos, así que se sustituye tal cual, **aunque el contador retroceda**.
- R33. **Modo cronómetro:** las vueltas son normales. **No hay lista de paradas**: hay un único valor y el forzaje actúa **una sola vez, en el primer Parar tras armar**; después se puede seguir parando e iniciando sin reset, como en una app real: el contador sigue corriendo sobre la misma vuelta y no se vuelve a forzar. Dos submodos:
  - *Solo centésimas*: el contador grande se detiene con las centésimas configuradas (0..99). Los minutos y segundos se quedan en los actuales o pasan al siguiente, nunca retroceden.
  - *Contador completo*: se configuran minutos (0..99), segundos (0..59) y centésimas; el contador se detiene exactamente en ese tiempo, tal cual.
- R34. Al parar con forzaje, la **vuelta en curso** se ajusta sola: vale contador − total de la última vuelta (nunca negativa: si el contador sustituido queda por debajo de la última vuelta, vale 0), así que las vueltas hechas más la vuelta en curso suman el contador. Al reanudar, el cronómetro sigue desde el valor forzado.
- R35. **Icono de reiniciar:** el círculo con punta de flecha lleva la punta en el extremo final del arco (sentido horario), no en el inicial.

## 6b. Icono y nombre de la app, guía, instrucciones y créditos
- R23. **Icono de la aplicación** (ajustes, apartado desplegable): lista de los 20 iconos de `data/icons` con miniatura y nombre. Por defecto, **Google / Android**. Al elegir otro sale un **modal de confirmación** (Cancelar / Aplicar). Android: un `activity-alias` por icono × nombre (80), solo uno habilitado; al aplicar se reinicia la app. iOS: icono alternativo (`AppIcon-<id>`); el propio iOS enseña un aviso del sistema y no se reinicia.
- R24. **Nombre de la aplicación** (ajustes, desplegable, solo Android): *Chronix* (por defecto), *Reloj*, *Clock*, *Cronómetro*. Mismo modal de confirmación y reinicio. iOS no permite cambiar el nombre en ejecución, así que ahí no aparece.
- R25. Los iconos y los alias se regeneran con `python tools/gen_icons.py` (lee `data/icons/PNG`).
- R26. **Guía de inicio:** ventana de varias páginas (qué es, cómo se usa, cómo abrir los ajustes, el forzaje, y un cierre) que sale al abrir la app mientras esté activa. Activa por defecto al instalar. En la última página: *No volver a mostrar* o *Cerrar*.
- R27. Ajuste **«Ocultar guía de inicio»** y botón **«Ver guía»**. Si la guía sigue activa al entrar en los ajustes, sale un modal que recomienda desactivarla (*Desactivar guía* / *Ahora no*).
- R28. Pantalla **Instrucciones** (botón en los ajustes): descripción orientada al usuario de para qué sirve la app, el objetivo del efecto, el forzaje paso a paso, cómo abrir los ajustes y cada ajuste. Debe mantenerse al día cuando cambie una función o un ajuste.
- R29. Pie de los ajustes (y de las instrucciones): **«Desarrollado por WikiCode, Adri & Semi 2026»**.

## 7. Datos locales
- Ajustes secretos (tema, duración de pulsación, lista de centésimas, forzar en vuelta/total, icono, nombre, guía de inicio, interruptor de Magic Friends) en SharedPreferences / UserDefaults.
- Magic Friends guarda en el mismo almacén, con el prefijo `mf.`, la identidad anónima del dispositivo (`deviceId`/`deviceKey`) y la última instantánea de ajustes remotos. Solo se crean al activar Magic Friends.

## 8. Decisiones tomadas (resueltas con el usuario, 2026-11-16)
- Se fuerzan las centésimas de cada **vuelta** (tiempo total), no las de la pausa. El tiempo de vuelta sale por diferencia.
- Lista de posiciones ilimitada, 4 por defecto. Solo se fuerzan centésimas.
- Pestañas Alarma / Reloj mundial / Temporizador: no responden al toque en la fase 1.
- Colores de vueltas: la **primera** vuelta siempre en color normal; a partir de la 2.ª, la más corta en azul y la más larga en rojo (hacen falta al menos 2 vueltas coloreables).

## 9. Magic Friends: ajustes en remoto (decisiones del 2026-11-18)
Un **ayudante** cambia en directo, desde su móvil, los ajustes de predicción de este móvil (el **mago**). Servicio y librería
cliente en el repo hermano `magicfriends` (ver su `docs/DISENO.md` y `protocol/PROTOCOLO.md`; son la fuente de verdad del
protocolo). Lo de aquí es lo propio de Chronix. **Fase 1 (hecha): Chronix como mago.** **Fase 2 (hecha): Chronix como ayudante**,
con una **lista de conexiones** (MF11–MF18) sobre la librería 0.2, reutilizando la pantalla de forzaje. Fase 3 (pendiente): QR y cambiar la
caducidad de una sesión ya creada.

- MF1. **Interruptor «Usar Magic Friends»** en los ajustes (por defecto **apagado**). Es el interruptor *del móvil*: usar o no lo remoto.
  Solo con él activo se crea el cliente, se abre la red y se genera la identidad; apagado no hay red, ni cliente, ni identidad.
  Si la compilación no lleva configuración (MF9), el interruptor sale desactivado.
- MF2. **Capa remota separada de los ajustes locales** (los remotos nunca los machacan). Lista blanca de claves remotas
  (`RemoteKeys`): `forceMode`, `forceTarget`, `forcedCs`, `stopSubmode`, `stopCentis`, `stopFullCs`. **Nunca remotos:** aspecto, icono,
  nombre, `secretHoldMs`, guía, `showTotals` (el mago decide qué se ve: sin totales visibles se fuerza la vuelta aunque el ayudante pida el total).
  **Efectivo = `remoto[clave] ?: local[clave]`** con Magic Friends activo; si no, solo el local. El solapado entra en `AppSettings.forcePlan(remote)`,
  único sitio donde se construye el `ForcePlan`.
- MF3. **Formato en el cable** (JSON Merge Patch; un `null` quita el override y vuelve el local): `forceMode` `"LAPS"|"STOPWATCH"`;
  `forceTarget` `"LAP"|"TOTAL"`; `stopSubmode` `"CENTIS"|"FULL"`; `forcedCs` array de enteros 0..99 (una posición por vuelta);
  `stopCentis` entero 0..99; `stopFullCs` entero 0..`STOP_FULL_CS_MAX` (centésimas de segundo). Validación como en `SettingsStore.load()`:
  los números fuera de rango se **limitan**; un tipo erróneo, un enum desconocido o un `forcedCs` con algún elemento que no es número se **ignoran**
  (esa clave la decide el local; así las posiciones de las vueltas nunca se desplazan). Un `forcedCs` vacío es válido (sin forzaje por vueltas).
- MF4. **Cambio en vivo hasta el último instante.** Armar (pulsación larga en Restablecer) solo marca «armado»; el plan **no se copia**: el cronómetro
  lo pide a su origen en cada vuelta y en cada Parar (`Stopwatch.resetAndArm { … }`). El ayudante puede cambiar valores, e incluso el modo, tras armar.
  La vuelta n usa la posición n de la lista. El forzaje del Parar actúa una vez y desarma del todo. Si el origen falla, vale el último plan leído.
- MF5. **Sin red el efecto no falla.** La librería guarda la última instantánea remota (y respeta la caducidad con el reloj local); se usa al arrancar y sin conexión.
  La conexión se reintenta sola y un vigilante la reinicia si lleva 8 s caída. Los indicadores de conexión solo existen en la pantalla secreta de Magic Friends.
- MF6. **Aviso discreto:** vibra un pulso suave y breve (distinto del pulso firme de armar) cuando cambia lo que va a pasar: llega un cambio del ayudante
  (`ClientEvent.RemotePatch`) o un estado nuevo de la sesión activa (`ClientEvent.SettingsChanged`: al activarla con plantilla, al resincronizar o al salirse el ayudante).
- MF7. **Pantalla «Sesiones»** (fondo blanco como los ajustes): estado de la conexión; crear sesión (nombre ≤ 40, caducidad *Sin caducidad / 1 h / 24 h / 7 días*);
  código de emparejamiento (6 caracteres, un solo uso, 10 min) con «Nuevo código» (sirve para re-emparejar: quien lo use sustituye al ayudante anterior);
  solicitudes pendientes (Aceptar / Rechazar); por sesión: Activar / Pausar, Revocar (definitivo) y Borrar, con confirmación en los destructivos.
  **Solo una sesión activa a la vez**; *pausada* no es *caducada* ni *revocada*. Cambiar la caducidad de una sesión ya creada y el QR quedan para la fase 3.
- MF8. **Borrar** una sesión la retira al instante de la lista y deja de aplicar sus ajustes remotos (lo hace la librería 0.2; con la 0.1 hacía falta reconectar).
- MF9. **Configuración sensible** (el repo es público): URL del servidor y `appKey` **no** van en el código ni en ficheros versionados. Gradle los lee de
  `-Pmagicfriends.serverUrl` / `-Pmagicfriends.appKey`, del entorno (`MAGICFRIENDS_SERVER_URL` / `MAGICFRIENDS_APP_KEY`, secretos del repo en Actions) o de
  `local.properties`, y genera `MagicFriendsConfig.kt` en `build/`. Sin valores la app compila igual, con Magic Friends desactivado. La `appKey` va dentro del binario
  (protección débil, solo frena altas casuales de magos).
- MF10. **Plataformas:** Android declara el permiso `INTERNET` (generado por `tools/gen_icons.py`). iOS no necesita permisos (HTTPS/WSS). Las instrucciones y la guía de inicio explican Magic Friends.

### Chronix como ayudante: lista de conexiones (fase 2, decisiones del 2026-11-19)
- MF11. **Pantalla del ayudante** («Soy el ayudante» en los ajustes; visible si la compilación lleva la URL del servidor; no depende del interruptor del mago ni necesita la `appKey`).
  Dos niveles: la **lista de conexiones** y **una conexión**. Sin pantalla no hay red: en la lista solo se usa internet al unirse o al salir de una conexión; al entrar en una se abre
  su conexión (un cliente por mago, librería 0.2) y se cierra al volver. Un mismo móvil puede ser mago y ayudante. La identidad del ayudante (`mf.helper.*`) es única por dispositivo y la
  comparten todos los magos.
- MF12. **Lista de conexiones** (el espejo de la lista de sesiones del mago): cada sesión a la que el ayudante se une con un código, **del mismo mago o de varios**, queda en una lista local
  (`mfh.connections`: mago, sesión, nombre, último estado y cuándo se vio) que sobrevive a reiniciar la app. Cada tarjeta enseña el nombre, el estado (*Esperando a que el mago acepte*, *Activa*,
  *Pausada*, *Caducada*, o retirada con su motivo: rechazada, borrada, revocada, sustituido por otro ayudante, perdida), cuándo se vio y «Entrar» y «Salir» (o «Quitar»). Orden: activas,
  pausadas, pendientes, caducadas, retiradas. Al entrar en una conexión se refrescan con el servidor **todas** las sesiones de ese mago; las que el servidor muestra y no estaban en la lista se añaden.
  Las conexiones hechas con la versión anterior (un solo mago, sin lista) no se importan: hay que volver a unirse.
- MF13. **Unirse:** código de 6 caracteres + nombre (≤ 24, lo ve el mago). `join` devuelve el mago, la sesión y su nombre, así que la tarjeta aparece al momento como *Esperando a que el mago acepte*.
  Errores traducidos (código no válido o ya usado, demasiados intentos, sesión caducada o llena, sin conexión).
- MF14. **Una conexión:** al entrar se conecta con el mago de esa sesión y enseña su **estado** (activa, pausada, caducada…) y **su plantilla**, el plan guardado en esa sesión del servidor. Cada conexión
  tiene la suya, así que pasar de una a otra no pierde nada. Si ya no es válida (rechazada, retirada, borrada, o el servidor ya no reconoce al dispositivo) se dice por qué y se quita con «Quitar de mi lista».
- MF15. **Editor reutilizado:** `ForcingEditor` es el mismo bloque de forzaje de los ajustes locales, parametrizado por `ForcingValues` (las seis claves de la lista blanca), con textos propios del ayudante.
  «Tiempo total» siempre se puede elegir; si el móvil del mago no muestra totales, allí se fuerza la vuelta (MF2).
- MF16. **Plan completo, envío explícito y plantilla editable en pausa:** el ayudante manda siempre las **seis claves** (`ForcingValues.toPatch()`, exactamente lo que acepta `parseRemoteOverrides`) con
  un botón y nunca por tecla (así no llegan valores a medias ni vibraciones repetidas al mago). Con la sesión **activa** el botón es **«Enviar»** (llega ya al móvil del mago); con la **pausada**,
  **«Guardar plantilla»** (se guarda en esa conexión y el mago no la recibe hasta activarla). **«Soltar el control»** / **«Vaciar plantilla»** ponen `null` en las seis claves. Una vez hay plan, el botón
  solo se habilita con cambios. Con la sesión caducada o sin conexión no se puede guardar nada (se dice por qué). El borrador sigue lo que haya en el servidor mientras el ayudante no lo toque.
- MF17. **Salir = eliminar la conexión:** «Salir de la sesión» (dentro de la conexión) y «Salir» (en la tarjeta, abriendo una conexión temporal con ese mago) envían `session.leave`: el ayudante deja de ser
  el controlador, **se borra la plantilla** de esa sesión (el móvil del mago vuelve a sus ajustes y vibra, `SettingsChanged`) y la conexión desaparece de la lista. La sesión sigue siendo del mago. Sin conexión se ofrece
  quitarla solo de la lista (el mago seguirá viéndole como ayudante). «Quitar» en una conexión *pendiente* también intenta salir en el servidor, por si el mago ya la había aceptado. El ayudante no borra sesiones: eso es del mago.
- MF18. Limitaciones: la lista es local (otro móvil del mismo ayudante tiene la suya); el estado de cada tarjeta es el de la última vez que se entró en ese mago; el QR y cambiar la caducidad de una sesión, para la fase 3.
