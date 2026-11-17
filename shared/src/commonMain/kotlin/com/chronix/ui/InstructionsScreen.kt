package com.chronix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Texto del pie de los ajustes y de las instrucciones. */
const val CREDITS = "Desarrollado por WikiCode, Adri & Semi 2026"

/**
 * Instrucciones para el usuario: para qué sirve la app, el forzaje y cada ajuste.
 * Mantener al día cuando cambie una función o un ajuste (regla del CLAUDE.md).
 */
@Composable
fun InstructionsScreen(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color.White).windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Volver", fontSize = 18.sp) }
        }
        Text("Instrucciones", fontSize = 28.sp, color = Color.Black)

        H("Para qué sirve")
        P("Chronix es una herramienta para hacer un efecto de magia. Parece la aplicación de reloj de un móvil, con su cronómetro, pero en realidad te permite decidir con qué centésimas terminan las vueltas.")
        P("El cronómetro funciona de forma totalmente normal: Iniciar, Vuelta, Parar, Reanudar y Restablecer. Las pestañas de abajo (Alarma, Reloj mundial, Temporizador) son solo de adorno y no hacen nada.")

        H("El objetivo del efecto")
        P("Quien mira el cronómetro ve vueltas que terminan en unas centésimas «imposibles de controlar»: por ejemplo 01, 22, 33 y 04, aunque pares el cronómetro cuando quieras. Esas centésimas las eliges tú de antemano.")

        H("Cómo se hace el forzaje")
        P("1. Entra en los ajustes y elige el modo de forzaje y los números (dos cifras cada uno). Hay dos modos, que se explican más abajo.")
        P("2. Con el cronómetro en cero o en pausa, mantén pulsado el botón Restablecer (en algunos diseños, el icono de reiniciar). El móvil vibra: el forzaje queda armado y el cronómetro se pone a cero. No se ve nada en pantalla.")
        P("3. Usa el cronómetro como quieras. El forzaje actúa según el modo elegido.")
        P("4. Cuando el forzaje ya ha actuado todo lo que tenía que hacer, termina solo y el cronómetro vuelve a ser completamente normal. Un Restablecer corto también lo cancela.")
        P("Regla de oro: lo que se fuerza se sustituye siempre por tus números. Lo que no se fuerza nunca viaja atrás: si solo se fuerzan las centésimas, los segundos se quedan en los que estás o pasan al siguiente, pero nunca retroceden.")

        H("Los dos modos de forzaje")
        S("Por vueltas")
        P("Cada vez que pulsas Vuelta se fuerzan las centésimas de esa vuelta con la siguiente cifra de tu lista (vuelta 1, vuelta 2…). Cuando se acaba la lista, las vueltas vuelven a ser normales.")
        S("Cronómetro (al pulsar Parar)")
        P("Las vueltas son normales y no hay lista: el forzaje actúa una sola vez, la primera vez que pulsas Parar tras armarlo. Después puedes seguir parando e iniciando como en cualquier cronómetro: el contador sigue corriendo sobre la misma vuelta, sin reiniciarse, y no se vuelve a forzar.")
        P("• «Solo centésimas»: el contador grande se detiene con las centésimas que escribes. Los minutos y segundos no se fuerzan: se quedan en los que estás o pasan al siguiente.")
        P("• «Contador completo»: escribes minutos, segundos y centésimas y el contador se detiene exactamente en ese tiempo, tal cual, aunque tenga que retroceder.")
        P("En ambos casos la vuelta en curso (el tiempo pequeño bajo el contador) se ajusta sola para que cuadre con las vueltas hechas y el contador.")

        H("Cómo abrir los ajustes")
        P("No hay ningún botón de ajustes. Mantén pulsado (1,5 segundos por defecto) en cualquier zona vacía de la pantalla, donde no haya botones. Deja el dedo quieto: si lo mueves, se cancela.")

        H("Los ajustes, uno a uno")
        S("Look and feel")
        P("Elige el aspecto del reloj: Samsung, iPhone, Pixel, OnePlus, OPPO, Xiaomi o Huawei. Todos funcionan igual por dentro. En Samsung y iPhone puedes deslizar para ver el reloj analógico.")
        S("Mostrar tiempos totales")
        P("Algunos diseños enseñan en cada vuelta el tiempo de vuelta y el tiempo total. Si desactivas esta opción, solo se ve el tiempo de vuelta. En el diseño del iPhone no hay columna de totales, así que no aplica.")
        S("Modo de forzaje")
        P("«Por vueltas» o «Cronómetro (al pulsar Parar)», como se explica arriba.")
        S("Forzar las centésimas en (modo por vueltas)")
        P("«Tiempo de vuelta» (lo normal): las centésimas que escribes aparecen en el tiempo de cada vuelta, y el total sale como consecuencia. «Tiempo total»: se fuerzan en el total de cada vuelta. Si los totales no se ven (diseño sin totales o totales ocultos), «Tiempo total» no está disponible y se usa «Tiempo de vuelta».")
        S("Qué forzar al parar (modo cronómetro)")
        P("«Solo centésimas del contador grande» o «Contador completo (minutos, segundos y centésimas)», y debajo los números que quieres. Es un único valor: no hay lista.")
        S("Pulsación larga para abrir los ajustes")
        P("Cuánto hay que mantener pulsado en una zona vacía para entrar en los ajustes. De 0,5 a 5 segundos.")
        S("Centésimas forzadas por vuelta")
        P("La lista de números del modo por vueltas. Vienen 4 por defecto; con «Añadir vuelta» agregas una más y con «Quitar» eliminas una.")
        S("Icono de la aplicación")
        P("Despliega la lista de iconos (Samsung, iPhone, Pixel…) y elige el que quieras para el lanzador del móvil. Antes de cambiarlo te avisa y te pide confirmación. En Android la app se reinicia para aplicarlo; en iPhone, el sistema muestra su propio aviso.")
        S("Nombre de la aplicación")
        P("Solo en Android. Elige cómo se llama la app bajo el icono: Chronix, Reloj, Clock o Cronómetro. También pide confirmación y reinicia la app.")
        S("Guía de inicio")
        P("La guía que aparece al abrir la app. «Ocultar guía de inicio» la desactiva; «Ver guía» te la enseña de nuevo cuando quieras. Mientras esté activa, al entrar en los ajustes se te recomienda desactivarla.")

        H("Consejos")
        P("• Prepara los números antes del truco y comprueba el forzaje una vez.")
        P("• Si cierras la app, el forzaje armado se pierde: vuelve a armarlo con la pulsación larga en Restablecer.")
        P("• Si no vibra, comprueba que el móvil no esté en modo silencio con la vibración desactivada.")

        Spacer(Modifier.height(32.dp))
        Text(CREDITS, fontSize = 13.sp, color = Color(0xFF7A7A80), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun H(text: String) {
    Spacer(Modifier.height(24.dp))
    Text(text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF3F3FE0))
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun S(text: String) {
    Spacer(Modifier.height(10.dp))
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
}

@Composable
private fun P(text: String) {
    Text(text, fontSize = 15.sp, lineHeight = 22.sp, color = Color(0xFF222226), modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
}
