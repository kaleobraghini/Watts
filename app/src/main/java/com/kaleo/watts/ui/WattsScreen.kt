package com.kaleo.watts.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaleo.watts.BatterySample
import com.kaleo.watts.ChargeState
import com.kaleo.watts.CurrentUnit
import com.kaleo.watts.WattsViewModel
import java.util.Locale
import kotlin.math.abs

object C {
    val Bg = Color(0xFF121018)
    val Surface = Color(0xFF1C1A27)
    val Line = Color(0xFF24212F)
    val Muted = Color(0xFFA9A4BF)
    val Soft = Color(0xFFD2CEE3)
    val Text = Color(0xFFF1EFF8)
    val Accent = Color(0xFF6D4FF2)
    val AccentText = Color(0xFFC4B8FF)
    val Green = Color(0xFF3DDC97)
    val Amber = Color(0xFFF5C451)
    val Red = Color(0xFFFF8A8A)
}

private val BR = Locale("pt", "BR")
private fun f(v: Double, d: Int) = String.format(BR, "%.${d}f", v)

private fun duration(ms: Long): String {
    val m = ms / 60000
    return if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min"
}

@Composable
fun WattsScreen(vm: WattsViewModel) {
    val s by vm.sample.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val consumption by vm.consumptionW.collectAsStateWithLifecycle()
    var settings by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(C.Bg).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Watts", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.Soft, modifier = Modifier.weight(1f))
            IconButton(onClick = { settings = true }) { Icon(Icons.Filled.Settings, "Ajustes", tint = C.Soft) }
        }

        val sample = s
        if (sample == null) {
            Text("Lendo a bateria…", color = C.Muted)
        } else {
            Hero(sample, consumption, vm.chargerWatts(sample.watts))
            Chart(history)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("Bateria", "${sample.level}%", Modifier.weight(1f))
                Tile("Temperatura", f(sample.tempC, 1) + " °C", Modifier.weight(1f), if (sample.tempC >= 40) C.Amber else C.Text)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("Fonte", sample.source, Modifier.weight(1f))
                Tile(
                    if (sample.fullInMs != null) "Cheio em" else "Saúde",
                    sample.fullInMs?.let { duration(it) } ?: sample.health,
                    Modifier.weight(1f),
                )
            }

            Column(Modifier.fillMaxWidth().background(C.Surface, RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Esta carga", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TextButton(onClick = vm::resetSession) { Text("Zerar", color = C.AccentText) }
                }
                Row {
                    Stat("Pico", f(session.peakW, 1) + " W", Modifier.weight(1f))
                    Stat("Média", f(session.averageW, 1) + " W", Modifier.weight(1f))
                }
                Row {
                    Stat("Energia", f(session.energyWh, 2) + " Wh", Modifier.weight(1f))
                    Stat("Tempo", duration((session.seconds * 1000).toLong()), Modifier.weight(1f))
                }
            }

            if (!sample.currentSupported) {
                Text("Este aparelho não informa a corrente da bateria, então os watts não podem ser calculados.", color = C.Amber, fontSize = 13.sp)
            }
            Text(
                "O Android mede só a bateria. Fora da tomada, isso é o consumo exato do celular. Na tomada, o app soma o que entra na bateria " +
                    "com o último consumo medido para estimar o total do carregador (≈). Perto de 80–100% a carga diminui de propósito.",
                color = C.Muted, fontSize = 12.sp, lineHeight = 17.sp,
            )
        }
    }

    if (settings) SettingsDialog(vm) { settings = false }
}

@Composable
private fun BigWatts(label: String, value: String, color: Color) {
    Column {
        Text(label, color = C.Muted, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 64.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = (-2).sp)
            Text("W", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = C.Muted, modifier = Modifier.padding(start = 6.dp, bottom = 12.dp))
        }
    }
}

@Composable
private fun Flow(label: String, value: String, color: Color, hint: String, modifier: Modifier) {
    Column(modifier.background(C.Surface, RoundedCornerShape(16.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Text(label, fontSize = 12.sp, color = C.Muted, modifier = Modifier.padding(start = 6.dp))
        }
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.padding(top = 4.dp))
        Text(hint, fontSize = 11.sp, color = C.Muted)
    }
}

/**
 * Separa as três coisas: o que o carregador entrega, o que vai para a bateria e o que o celular gasta.
 * Na tomada, o consumo do celular é estimado (o Android só mede a bateria).
 */
@Composable
private fun Hero(s: BatterySample, consumption: Double?, charger: Double?) {
    val plugged = s.source != "Desconectado"
    val (label, color) = when (s.state) {
        ChargeState.CHARGING -> "Carregando · ${s.source}" to C.Green
        ChargeState.FULL -> "Carga completa · ${s.source}" to C.Green
        ChargeState.NOT_CHARGING -> "Conectado, sem carregar" to C.Amber
        ChargeState.DISCHARGING -> if (plugged) "Conectado, mas descarregando" to C.Amber else "Na bateria" to C.Muted
        ChargeState.UNKNOWN -> "Desconhecido" to C.Muted
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Text(label, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
        }
        if (plugged) {
            BigWatts("Carregador entregando", charger?.let { "≈" + f(it, 1) } ?: "—", C.Text)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Flow("Indo para a bateria", f(maxOf(0.0, s.watts), 1) + " W", C.Green, "medido", Modifier.weight(1f))
                Flow("Celular gastando", consumption?.let { "≈" + f(it, 1) + " W" } ?: "—", C.Red, if (consumption != null) "estimado" else "sem medição ainda", Modifier.weight(1f))
            }
            if (consumption == null) Text(
                "Para estimar o consumo do celular (e o total do carregador), tire da tomada por cerca de 1 minuto com o app aberto.",
                color = C.Amber, fontSize = 12.sp, lineHeight = 17.sp,
            )
        } else {
            BigWatts("Celular gastando", f(abs(s.watts), 1), C.Red)
            Flow("Carregador", "Desconectado", C.Muted, "conecte para ver quanto ele entrega", Modifier.fillMaxWidth())
        }
        Text(
            f(s.milliVolts / 1000.0, 2) + " V × " + f(abs(s.microAmps) / 1_000_000.0, 2) + " A na bateria",
            color = C.Muted, fontSize = 13.sp,
        )
    }
}

@Composable
private fun Chart(values: List<Double>) {
    val max = (values.maxOfOrNull { abs(it) } ?: 0.0).coerceAtLeast(5.0)
    Column(Modifier.fillMaxWidth().background(C.Surface, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Row {
            Text(if ((values.lastOrNull() ?: 0.0) >= 0) "Bateria · entrando · 2 min" else "Bateria · saindo · 2 min", fontSize = 13.sp, color = C.Muted, modifier = Modifier.weight(1f))
            Text("máx. " + f(max, 0) + " W", fontSize = 13.sp, color = C.Muted)
        }
        Canvas(Modifier.fillMaxWidth().height(140.dp).padding(top = 12.dp)) {
            val w = size.width
            val h = size.height
            listOf(0.5f, 1f).forEach { y -> drawLine(C.Line, Offset(0f, h * y), Offset(w, h * y), strokeWidth = 2f) }
            if (values.size < 2) return@Canvas
            val step = w / 119f
            val start = w - step * (values.size - 1)
            val line = Path()
            values.forEachIndexed { i, v ->
                val x = start + step * i
                val y = h - (abs(v) / max).toFloat() * h
                if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
            }
            val area = Path().apply { addPath(line); lineTo(w, h); lineTo(start, h); close() }
            val lineColor = if (values.last() >= 0) C.Green else C.Red
            drawPath(area, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.30f), Color.Transparent)))
            drawPath(line, lineColor, style = Stroke(width = 5f))
        }
    }
}

@Composable
private fun Tile(label: String, value: String, modifier: Modifier, color: Color = C.Text) {
    Column(modifier.background(C.Surface, RoundedCornerShape(16.dp)).padding(14.dp)) {
        Text(label, fontSize = 12.sp, color = C.Muted)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = color, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, fontSize = 12.sp, color = C.Muted)
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsDialog(vm: WattsViewModel, onDismiss: () -> Unit) {
    val unit by vm.unit.collectAsStateWithLifecycle()
    val screenOn by vm.keepScreenOn.collectAsStateWithLifecycle()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Surface,
        title = { Text("Ajustes") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Manter tela ligada", modifier = Modifier.weight(1f))
                    Switch(screenOn, vm::setKeepScreenOn, colors = SwitchDefaults.colors(checkedTrackColor = C.Accent))
                }
                Text("Unidade da corrente", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp))
                Text("Se os watts parecerem 1000 vezes maiores ou menores, troque aqui.", fontSize = 12.sp, color = C.Muted)
                CurrentUnit.entries.forEach { u ->
                    Row(Modifier.fillMaxWidth().clickable { vm.setUnit(u) }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(unit == u, onClick = { vm.setUnit(u) }, colors = RadioButtonDefaults.colors(selectedColor = C.Accent))
                        Text(u.label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar", color = C.AccentText) } },
    )
}
