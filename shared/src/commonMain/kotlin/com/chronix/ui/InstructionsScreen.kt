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
        P("1. Entra en los ajustes y escribe, en «Centésimas forzadas por vuelta», los números que quieres para la vuelta 1, 2, 3, 4… (dos cifras cada uno). Vienen 4 por defecto y puedes añadir tantas como quieras.")
        P("2. Con el cronómetro en cero o en pausa, mantén pulsado el botón Restablecer (en algunos diseños, el icono de reiniciar). El móvil vibra: el forzaje queda armado y el cronómetro se pone a cero. No se ve nada en pantalla.")
        P("3. Pulsa Iniciar y ve marcando vueltas. Cada vuelta terminará en las centésimas que pusiste, en el orden de tu lista.")
        P("4. Cuando se acaban los números de la lista, el forzaje termina solo y el cronómetro vuelve a ser completamente normal. Un Restablecer corto también lo cancela.")
        P("Los segundos nunca se restan: el tiempo mostrado se queda en el segundo en que estás o pasa al siguiente, nunca va por detrás del tiempo real.")

        H("Cómo abrir los ajustes")
        P("No hay ningún botón de ajustes. Mantén pulsado (1,5 segundos por defecto) en cualquier zona vacía de la pantalla, donde no haya botones. Deja el dedo quieto: si lo mueves, se cancela.")

        H("Los ajustes, uno a uno")
        S("Look and feel")
        P("Elige el aspecto del reloj: Samsung, iPhone, Pixel, OnePlus, OPPO, Xiaomi o Huawei. Todos funcionan igual por dentro. En Samsung y iPhone puedes deslizar para ver el reloj analógico.")
        S("Forzar las centésimas en")
        P("«Tiempo de vuelta» (lo normal): las centésimas que escribes aparecen en el tiempo de cada vuelta, y el total sale como consecuencia. «Tiempo total»: se fuerzan en el total de cada vuelta. Si el diseño no enseña el total (el del iPhone), te avisa de que no se verá.")
        S("Pulsación larga para abrir los ajustes")
        P("Cuánto hay que mantener pulsado en una zona vacía para entrar en los ajustes. De 0,5 a 5 segundos.")
        S("Centésimas forzadas por vuelta")
        P("La lista de números del forzaje. Con «Añadir vuelta» agregas una más y con «Quitar» eliminas una.")
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
