# Chronix · Especificación funcional

Fuente de verdad funcional. Los requisitos están numerados; las dudas abiertas, al final.

## 1. Concepto
Herramienta para un **efecto de magia**. Emula una app de reloj real (cronómetro, alarmas, reloj mundial, temporizador)
para que parezca auténtica. Solo el **cronómetro** es realista y funcional en la fase 1. La app permite **forzar las
centésimas** de las vueltas y de la parada a valores preparados de antemano.

- Plataformas: Android + iOS (un único código KMP). Orientación vertical. Idioma: español.
- Todo en local. Sin red ni cuentas.

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
- R15. Acceso: **pulsación larga en la esquina superior derecha** (zona invisible). Duración por defecto **1,5 s**, configurable.
- R16. Contenido (fase 1; las cajas de centésimas son de texto numérico normal, se pueden borrar y el teclado se cierra al tocar fuera):
  - Elegir el look and feel.
  - Duración de la pulsación larga de acceso.
  - **Lista de centésimas forzadas** por vuelta (vuelta 1, 2, 3, 4…): dos dígitos cada una.
  - Vibración al armar el forzaje (activar/desactivar, por confirmar).

## 6. Modo forzaje (el efecto)
- R17. **Armado:** pulsación larga (~0,8 s) sobre **Restablecer** → el cronómetro se pone a cero, vibra el móvil y queda armado el forzaje. Sin ningún cambio visual.
- R18. Los ajustes guardan una **lista de centésimas** (Vuelta 1: 45, Vuelta 2: 96…). Con el forzaje armado, al pulsar **Vuelta** la *n*-ésima vez, el **tiempo total** de esa vuelta termina en la centésima configurada para *n*, sea cual sea el minuto y segundo reales.
- R18b. **Coherencia:** el tiempo de vuelta se calcula por diferencia entre totales (vuelta = total − total anterior). Así la tabla cuadra siempre. El total forzado es el mayor valor con esa centésima que no supera el tiempo real; si no queda por encima del total anterior, sube de 1 en 1 segundo. Solo se tocan las centésimas (o el segundo mínimo imprescindible para mantener el orden).
- R18c. El forzaje solo actúa sobre las **vueltas**. La pausa muestra el tiempo real.
- R19. Cuando se han consumido todos los valores, el forzaje termina: las vueltas siguientes son normales.
- R20. **Restablecer** con pulsación corta pone a cero y desarma el forzaje.
- R21. El estado de armado no se guarda al cerrar la app.
- R22. Nada del forzaje debe ser visible ni detectable: sin textos, sin indicadores.
- R22b. La lista de ajustes viene por defecto con **4** posiciones (valor 00) y se puede ampliar **sin límite**.

## 7. Datos locales
- Ajustes secretos (tema, duración de pulsación, lista de centésimas) en SharedPreferences / UserDefaults.

## 8. Decisiones tomadas (resueltas con el usuario, 2026-11-16)
- Se fuerzan las centésimas de cada **vuelta** (tiempo total), no las de la pausa. El tiempo de vuelta sale por diferencia.
- Lista de posiciones ilimitada, 4 por defecto. Solo se fuerzan centésimas.
- Pestañas Alarma / Reloj mundial / Temporizador: no responden al toque en la fase 1.
- Colores de vueltas: la **primera** vuelta siempre en color normal; a partir de la 2.ª, la más corta en azul y la más larga en rojo (hacen falta al menos 2 vueltas coloreables).
