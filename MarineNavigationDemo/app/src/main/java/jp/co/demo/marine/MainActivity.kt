package jp.co.demo.marine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.co.demo.marine.ui.theme.MarineTheme
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MarineTheme {
                MarineApp()
            }
        }
    }
}

@Composable
fun MarineApp() {
    var running by remember { mutableStateOf(true) }
    var night by remember { mutableStateOf(true) }
    var radar by remember { mutableStateOf(true) }
    var warning by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(running) {
        while (running) {
            delay(1000)
            seconds++
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(if (night) Color(0xFF061521) else Color(0xFFE5F4F5))
            .padding(14.dp)
    ) {
        Header(running, seconds, night) { night = !night }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                Modifier
                    .weight(3.3f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(Modifier.fillMaxSize()) {
                    Chart(seconds, night, warning)
                    MapOverlay(Modifier.align(Alignment.TopStart))
                    if (warning) WarningBanner(Modifier.align(Alignment.TopCenter))
                }
            }
            Column(
                Modifier
                    .weight(1.25f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InstrumentPanel(seconds)
                if (radar) RadarPanel(Modifier.weight(1f)) else AisPanel(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(10.dp))
        Controls(
            running,
            { running = !running },
            radar,
            { radar = !radar },
            warning,
            { warning = !warning },
            night,
            { night = !night }
        )
    }
}

@Composable
private fun Header(running: Boolean, seconds: Int, night: Boolean, toggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Sailing,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(34.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text("MARINE NAVIGATION SYSTEM", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                "G510 OFFLINE UI DEMONSTRATION",
                color = Color(0xFF8CA8B7),
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.weight(1f))
        Pill("GPS", "FIX", Color(0xFF37DB91))
        Spacer(Modifier.width(7.dp))
        Pill("AIS", "ONLINE", Color(0xFF37DB91))
        Spacer(Modifier.width(7.dp))
        Pill(
            if (running) "NAV" else "PAUSE",
            formatTime(seconds),
            if (running) Color(0xFF27D3E2) else Color.Gray
        )
        IconButton(onClick = toggle) {
            Icon(
                if (night) Icons.Default.LightMode else Icons.Default.DarkMode,
                "表示切替"
            )
        }
        Text("DEMO", color = Color(0xFFFFB547), fontWeight = FontWeight.Black)
    }
}

@Composable
private fun Pill(a: String, b: String, c: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0xFF102736))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(c)
        )
        Spacer(Modifier.width(6.dp))
        Text("$a  $b", fontSize = 11.sp)
    }
}

@Composable
private fun Chart(t: Int, night: Boolean, warning: Boolean) {
    val sea = if (night) Color(0xFF092B3D) else Color(0xFF7CCED8)
    val grid = if (night) Color(0xFF2B6474) else Color.White.copy(.55f)
    Canvas(
        Modifier
            .fillMaxSize()
            .background(sea)
    ) {
        val step = size.minDimension / 10f
        var x = 0f
        while (x < size.width) {
            drawLine(grid.copy(.35f), Offset(x, 0f), Offset(x, size.height), 1f)
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(grid.copy(.35f), Offset(0f, y), Offset(size.width, y), 1f)
            y += step
        }
        val land = Path().apply {
            moveTo(0f, size.height * .18f)
            lineTo(size.width * .18f, size.height * .14f)
            lineTo(size.width * .28f, size.height * .27f)
            lineTo(size.width * .2f, size.height * .43f)
            lineTo(0f, size.height * .48f)
            close()
        }
        drawPath(land, Color(0xFF596B4B))
        drawPath(land, Color(0xFF9BC57A), style = Stroke(3f))
        val start = Offset(size.width * .34f, size.height * .78f)
        val mid = Offset(size.width * .56f, size.height * .53f)
        val end = Offset(size.width * .78f, size.height * .24f)
        val route = Path().apply {
            moveTo(start.x, start.y)
            quadraticBezierTo(mid.x, mid.y, end.x, end.y)
        }
        drawPath(
            route,
            Color(0xFFFFD166),
            style = Stroke(5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f)))
        )
        drawCircle(Color(0xFFFFD166), 10f, end)
        drawCircle(Color.White, 16f, end, style = Stroke(3f))
        val p = (t % 60) / 60f
        val ship = Offset(start.x + (end.x - start.x) * p, start.y + (end.y - start.y) * p)
        val shipPath = Path().apply {
            moveTo(ship.x, ship.y - 18f)
            lineTo(ship.x - 12f, ship.y + 14f)
            lineTo(ship.x, ship.y + 8f)
            lineTo(ship.x + 12f, ship.y + 14f)
            close()
        }
        drawPath(shipPath, Color.White)
        drawCircle(Color(0xFF27D3E2), 24f, ship, style = Stroke(3f))
        listOf(
            Offset(size.width * .64f, size.height * .71f),
            Offset(size.width * .82f, size.height * .55f),
            Offset(size.width * .48f, size.height * .22f)
        ).forEachIndexed { i, p2 ->
            drawCircle(if (i == 0 && warning) Color.Red else Color(0xFFFFB547), 8f, p2)
            drawCircle(Color.White.copy(.7f), 16f, p2, style = Stroke(2f))
        }
    }
}

@Composable
private fun MapOverlay(m: Modifier) {
    Column(
        m
            .padding(12.dp)
            .background(Color.Black.copy(.45f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text("ROUTE 01", fontWeight = FontWeight.Bold)
        Text("YOKOHAMA → TOKYO BAY", fontSize = 11.sp, color = Color.LightGray)
    }
}

@Composable
private fun WarningBanner(m: Modifier) {
    Row(
        m
            .padding(14.dp)
            .background(Color(0xFFD62F3A), RoundedCornerShape(8.dp))
            .padding(12.dp, 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Warning, null)
        Spacer(Modifier.width(6.dp))
        Text("CPA WARNING: TARGET 01", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InstrumentPanel(t: Int) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Text("航行データ", fontWeight = FontWeight.Bold)
            HorizontalDivider(
                Modifier.padding(vertical = 8.dp),
                color = Color.White.copy(.1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("SOG", "${"%.1f".format(12.2 + (t % 8) * .1)} kt", Modifier.weight(1f))
                Metric("COG", "235°", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("DIST", "3.2 nm", Modifier.weight(1f))
                Metric("ETA", "14:25", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("WIND", "12 kt", Modifier.weight(1f))
                Metric("WAVE", "1.2 m", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Metric(a: String, b: String, m: Modifier) {
    Column(
        m
            .background(Color(0xFF18384A), RoundedCornerShape(8.dp))
            .padding(9.dp)
    ) {
        Text(a, fontSize = 10.sp, color = Color(0xFF8CA8B7))
        Text(b, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RadarPanel(m: Modifier) {
    val angle by rememberInfiniteTransition(label = "radar").animateFloat(
        0f,
        360f,
        infiniteRepeatable(tween(3500, easing = LinearEasing)),
        label = "angle"
    )
    Card(m) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Text("RADAR", fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension * .43f
                    val c = center
                    repeat(4) { i ->
                        drawCircle(Color(0xFF27D3E2).copy(.28f), r * (i + 1) / 4, c, style = Stroke(1.5f))
                    }
                    drawLine(
                        Color(0xFF27D3E2).copy(.25f),
                        Offset(c.x - r, c.y),
                        Offset(c.x + r, c.y)
                    )
                    drawLine(
                        Color(0xFF27D3E2).copy(.25f),
                        Offset(c.x, c.y - r),
                        Offset(c.x, c.y + r)
                    )
                    val a = Math.toRadians(angle.toDouble())
                    val end = Offset(c.x + cos(a).toFloat() * r, c.y + sin(a).toFloat() * r)
                    drawLine(Color(0xFF39F7A5), c, end, 4f)
                    listOf(
                        Offset(c.x + r * .4f, c.y - r * .25f),
                        Offset(c.x - r * .55f, c.y + r * .3f)
                    ).forEach { drawCircle(Color(0xFFFFB547), 6f, it) }
                }
            }
        }
    }
}

@Composable
private fun AisPanel(m: Modifier) {
    Card(m) {
        Column(Modifier.padding(12.dp)) {
            Text("AIS TARGETS", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Target("01", "CARGO A", "1.2 nm", "CPA 0.3")
            Target("02", "FERRY B", "2.3 nm", "CPA 1.1")
            Target("03", "TUG C", "3.8 nm", "CPA 2.0")
        }
    }
}

@Composable
private fun Target(id: String, n: String, d: String, cpa: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E5264)),
            contentAlignment = Alignment.Center
        ) {
            Text(id, fontSize = 10.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(n, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(cpa, fontSize = 10.sp, color = Color.Gray)
        }
        Text(d, fontSize = 11.sp)
    }
}

@Composable
private fun Controls(
    running: Boolean,
    toggleRun: () -> Unit,
    radar: Boolean,
    toggleRadar: () -> Unit,
    warning: Boolean,
    toggleWarning: () -> Unit,
    night: Boolean,
    toggleNight: () -> Unit
) {
    Card {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = toggleRun) {
                Icon(if (running) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(5.dp))
                Text(if (running) "航行停止" else "航行開始")
            }
            OutlinedButton(onClick = toggleRadar) {
                Icon(if (radar) Icons.Default.List else Icons.Default.Radar, null)
                Spacer(Modifier.width(5.dp))
                Text(if (radar) "AIS一覧" else "レーダー")
            }
            FilterChip(
                selected = warning,
                onClick = toggleWarning,
                label = { Text("衝突警告デモ") },
                leadingIcon = { Icon(Icons.Default.Warning, null) }
            )
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = toggleNight) {
                Icon(if (night) Icons.Default.LightMode else Icons.Default.DarkMode, null)
                Spacer(Modifier.width(5.dp))
                Text(if (night) "昼間" else "夜間")
            }
            Text(
                "SIMULATION ONLY / NOT FOR NAVIGATION",
                fontSize = 10.sp,
                color = Color(0xFFFFB547),
                textAlign = TextAlign.End
            )
        }
    }
}

private fun formatTime(s: Int) = "%02d:%02d".format(s / 60, s % 60)
