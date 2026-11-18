package com.chronix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

private class GuidePage(val title: String, val lines: List<String>)

private val guidePages = listOf(
    GuidePage(
        "Bienvenido a Chronix",
        listOf(
            "Chronix es una herramienta para un efecto de magia.",
            "Por fuera es un cronómetro normal y corriente, con el aspecto del reloj de tu móvil o el de otra marca.",
            "Por dentro esconde el forzaje: te deja decidir con qué centésimas terminan las vueltas.",
        ),
    ),
    GuidePage(
        "Cómo se usa",
        listOf(
            "El cronómetro funciona de verdad: Iniciar, Vuelta, Parar, Reanudar y Restablecer.",
            "Las pestañas de abajo (Alarma, Reloj, Temporizador) son solo de adorno.",
            "Puedes cambiar el aspecto (Samsung, iPhone, Pixel, OnePlus, OPPO, Xiaomi o Huawei) desde los ajustes.",
        ),
    ),
    GuidePage(
        "Los ajustes están ocultos",
        listOf(
            "No hay ningún botón de ajustes a la vista, para que nadie los encuentre.",
            "Para abrirlos, mantén pulsado un par de segundos (1,5 s por defecto) en cualquier zona vacía de la pantalla, es decir, donde no haya botones.",
            "Tienes que dejar el dedo quieto: si lo mueves, se cancela. Ese tiempo se puede cambiar dentro de los ajustes.",
        ),
    ),
    GuidePage(
        "El forzaje",
        listOf(
            "1. En los ajustes elige el modo y escribe tus números (dos cifras cada uno).",
            "2. Con el cronómetro en pausa, mantén pulsado el botón Restablecer (o Reiniciar). El móvil vibra y queda armado, sin que se vea nada.",
            "3. Inicia el cronómetro y úsalo con normalidad. Cuando el forzaje ya ha actuado, termina solo y vuelve a ser un cronómetro normal.",
            "Lo que se fuerza se sustituye siempre por tus números; lo que no, nunca retrocede.",
        ),
    ),
    GuidePage(
        "Los dos modos",
        listOf(
            "Por vueltas: cada vez que pulsas Vuelta, esa vuelta termina en las centésimas de tu lista (vuelta 1, vuelta 2…).",
            "Cronómetro: las vueltas son normales y el forzaje actúa una sola vez, en el primer Parar. Puedes forzar solo las centésimas del contador grande o el contador completo (minutos, segundos y centésimas).",
            "Después sigues parando e iniciando como en cualquier cronómetro, sobre la misma vuelta.",
        ),
    ),
    GuidePage(
        "Magic Friends (opcional)",
        listOf(
            "Un ayudante puede cambiar tus números del forzaje desde su móvil, en directo, incluso después de armarlo.",
            "Se activa en los ajustes con «Usar Magic Friends». Con el interruptor apagado, la app no usa internet.",
            "Tus ajustes nunca se pisan: con Magic Friends activado vale lo que ponga el ayudante y, en lo que no toque, lo tuyo.",
        ),
    ),
    GuidePage(
        "¡Listo!",
        listOf(
            "Puedes volver a ver esta guía y las instrucciones completas cuando quieras desde los ajustes.",
            "¿Quieres que esta guía no vuelva a aparecer al abrir la app?",
        ),
    ),
)

/**
 * Guía de inicio: ventana con varias páginas que explica de qué va la app y cómo entrar en los ajustes. En la última
 * página se puede pedir que no vuelva a mostrarse ([onNeverShow]) o simplemente cerrarla ([onClose]).
 */
@Composable
fun GuideDialog(onClose: () -> Unit, onNeverShow: () -> Unit) {
    val pager = rememberPagerState(pageCount = { guidePages.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == guidePages.size - 1
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 48.dp), contentAlignment = Alignment.Center) {
            Surface(
                Modifier.fillMaxWidth().fillMaxHeight(0.68f),
                shape = RoundedCornerShape(28.dp), color = Color.White, shadowElevation = 8.dp,
            ) {
                Column(Modifier.padding(24.dp)) {
                    HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { i ->
                        val page = guidePages[i]
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Text("${i + 1} / ${guidePages.size}", fontSize = 13.sp, color = Color(0xFF7A7A80))
                            Spacer(Modifier.height(8.dp))
                            Text(page.title, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            Spacer(Modifier.height(16.dp))
                            page.lines.forEach {
                                Text(it, fontSize = 16.sp, lineHeight = 23.sp, color = Color(0xFF222226))
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        guidePages.indices.forEach { i ->
                            Box(
                                Modifier.padding(horizontal = 4.dp).size(8.dp).clip(CircleShape)
                                    .background(if (pager.currentPage == i) Color(0xFF3F3FE0) else Color(0x33000000)),
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (last) {
                        Button(onClick = onNeverShow, modifier = Modifier.fillMaxWidth()) { Text("No volver a mostrar") }
                        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onClose) { Text("Saltar") }
                            Button(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) { Text("Siguiente") }
                        }
                    }
                }
            }
        }
    }
}
