package jp.co.demo.g510homeenergymonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EnergyTheme { HomeEnergyMonitor() } }
    }
}

private val Bg = Color(0xFF06131B)
private val Panel = Color(0xFF0D2530)
private val Panel2 = Color(0xFF113442)
private val Mint = Color(0xFF27D9A2)
private val Yellow = Color(0xFFFFC857)
private val Blue = Color(0xFF45A9FF)
private val Red = Color(0xFFFF7272)
private val TextMain = Color(0xFFF2FAFC)
private val TextSub = Color(0xFF9DB4BD)

@Composable
private fun EnergyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = Mint, background = Bg, surface = Panel),
        typography = Typography(), content = content
    )
}

data class EnergyState(
    val solar: Float, val house: Float, val battery: Float,
    val ev: Float, val waterHeater: Float, val grid: Float,
    val batteryPercent: Int, val evPercent: Int, val weather: String,
    val temperature: Int, val forecast: List<Float>
)

private fun demoState(weatherIndex: Int, tick: Int): EnergyState {
    val conditions = listOf("晴れ", "くもり", "雨")
    val solarBase = listOf(4.8f, 2.5f, 0.9f)[weatherIndex]
    val wobble = ((tick % 8) - 4) * 0.07f
    val solar = (solarBase + wobble).coerceAtLeast(0.2f)
    val house = 2.1f + (tick % 5) * 0.08f
    val water = if (tick % 12 in 7..10) 1.4f else 0.4f
    val ev = if (tick % 16 in 0..8) 1.8f else 0.2f
    val demand = house + water + ev
    val battery = if (solar > demand) -(solar - demand) * 0.65f else (demand - solar) * 0.45f
    val grid = demand - solar - battery
    return EnergyState(
        solar, house, battery, ev, water, grid,
        78 + tick % 3, 62 + tick % 4, conditions[weatherIndex],
        listOf(28, 25, 22)[weatherIndex],
        List(12) { i -> (solarBase * (0.45f + 0.55f * sin((i + 1) * PI / 13))).toFloat() }
    )
}

@Composable
fun HomeEnergyMonitor() {
    var tick by remember { mutableIntStateOf(0) }
    var weatherIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1500); tick++ } }
    val s = demoState(weatherIndex, tick)

    Column(Modifier.fillMaxSize().background(Bg).padding(18.dp)) {
        Header(s, onWeather = { weatherIndex = (weatherIndex + 1) % 3 })
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            EnergyFlowPanel(s, Modifier.weight(1.65f).fillMaxHeight())
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ForecastPanel(s, Modifier.weight(1f).fillMaxWidth())
                SummaryPanel(s, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Header(s: EnergyState, onWeather: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("HOME ENERGY MONITOR", color = Mint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("スマートホーム電力フロー", color = TextMain, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(LocalTime.now().withSecond(0).withNano(0).toString(), color = TextSub, fontSize = 13.sp)
            Surface(
                color = Panel2, shape = RoundedCornerShape(22.dp),
                modifier = Modifier.clickable { onWeather() }
            ) {
                Text("${weatherIcon(s.weather)}  ${s.weather}  ${s.temperature}℃  │  タップで天候変更",
                    color = TextMain, modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp), fontSize = 14.sp)
            }
        }
    }
}

private fun weatherIcon(w: String) = when (w) { "晴れ" -> "☀"; "くもり" -> "☁"; else -> "☂" }

@Composable
private fun EnergyFlowPanel(s: EnergyState, modifier: Modifier = Modifier) {
    PanelBox(modifier) {
        Text("現在の電力フロー", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("発電・蓄電・消費をリアルタイム表示（デモデータ）", color = TextSub, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxSize()) {
            FlowLines(Modifier.fillMaxSize())
            EnergyNode("☀", "太陽光", s.solar, "発電", Yellow,
                Modifier.align(Alignment.TopCenter))
            EnergyNode("⌂", "家庭", s.house, "消費", Mint,
                Modifier.align(Alignment.Center))
            EnergyNode("▣", "蓄電池", kotlin.math.abs(s.battery),
                if (s.battery < 0) "充電 ${s.batteryPercent}%" else "放電 ${s.batteryPercent}%", Blue,
                Modifier.align(Alignment.CenterStart))
            EnergyNode("EV", "電気自動車", s.ev, "充電 ${s.evPercent}%", Color(0xFFB78CFF),
                Modifier.align(Alignment.CenterEnd))
            EnergyNode("♨", "給湯器", s.waterHeater, "使用中", Red,
                Modifier.align(Alignment.BottomStart))
            EnergyNode("▥", "電力会社", kotlin.math.abs(s.grid), if (s.grid >= 0) "買電" else "売電", Color(0xFF7DA0B0),
                Modifier.align(Alignment.BottomEnd))
        }
    }
}

@Composable
private fun FlowLines(modifier: Modifier) {
    val trans = rememberInfiniteTransition(label = "flow")
    val phase by trans.animateFloat(0f, 1f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "phase")
    Canvas(modifier.padding(horizontal = 72.dp, vertical = 55.dp)) {
        val c = Offset(size.width / 2, size.height / 2)
        val points = listOf(
            Offset(size.width / 2, 4f), Offset(6f, size.height / 2), Offset(size.width - 6f, size.height / 2),
            Offset(8f, size.height - 8f), Offset(size.width - 8f, size.height - 8f)
        )
        points.forEach { p ->
            drawLine(Color(0xFF244652), c, p, 5f, cap = StrokeCap.Round)
            val q = Offset(c.x + (p.x - c.x) * phase, c.y + (p.y - c.y) * phase)
            drawCircle(Mint, 7f, q)
        }
    }
}

@Composable
private fun EnergyNode(icon: String, title: String, kw: Float, status: String, accent: Color, modifier: Modifier) {
    Surface(modifier = modifier.width(126.dp), shape = RoundedCornerShape(18.dp), color = Panel2,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = .55f))) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, color = accent, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(title, color = TextSub, fontSize = 11.sp)
            Text("%.1f kW".format(kw), color = TextMain, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(status, color = accent, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ForecastPanel(s: EnergyState, modifier: Modifier = Modifier) {
    PanelBox(modifier) {
        Text("AI発電予測", color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("天候シナリオに基づく展示用予測", color = TextSub, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        val max = (s.forecast.maxOrNull() ?: 1f).coerceAtLeast(1f)
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val gap = size.width / s.forecast.size
            s.forecast.forEachIndexed { i, v ->
                val h = size.height * v / max * .82f
                drawRoundRect(Mint.copy(alpha = .75f),
                    topLeft = Offset(i * gap + gap * .15f, size.height - h),
                    size = androidx.compose.ui.geometry.Size(gap * .7f, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("6:00", color = TextSub, fontSize = 10.sp); Text("12:00", color = TextSub, fontSize = 10.sp); Text("18:00", color = TextSub, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SummaryPanel(s: EnergyState, modifier: Modifier = Modifier) {
    PanelBox(modifier) {
        Text("本日のサマリー", color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("発電量", "18.6 kWh", Mint, Modifier.weight(1f))
            Metric("自給率", "76 %", Yellow, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric("CO₂削減", "7.9 kg", Blue, Modifier.weight(1f))
            Metric("節約額", "512 円", Color(0xFFB78CFF), Modifier.weight(1f))
        }
        Spacer(Modifier.height(9.dp))
        val advice = when (s.weather) {
            "晴れ" -> "午後の余剰電力でEV充電を推奨"
            "くもり" -> "蓄電池を残し、ピーク時間帯に活用"
            else -> "買電増加を抑えるため給湯時刻を調整"
        }
        Text("AIアドバイス", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(advice, color = TextMain, fontSize = 12.sp)
    }
}

@Composable
private fun Metric(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier.background(color.copy(alpha = .12f), RoundedCornerShape(12.dp)).padding(9.dp)) {
        Text(label, color = TextSub, fontSize = 10.sp)
        Text(value, color = color, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PanelBox(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier, color = Panel, shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B3C48))) {
        Column(Modifier.fillMaxSize().padding(16.dp), content = content)
    }
}
