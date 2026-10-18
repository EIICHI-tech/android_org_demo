package jp.co.demo.endoscope

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.co.demo.endoscope.ui.theme.EndoscopeTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EndoscopeTheme { EndoscopeDemoApp() } }
    }
}

@Composable
fun EndoscopeDemoApp() {
    var recording by remember { mutableStateOf(false) }
    var elapsed by remember { mutableIntStateOf(0) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var captured by remember { mutableIntStateOf(0) }
    var compare by remember { mutableStateOf(false) }
    var enhancement by remember { mutableStateOf(true) }

    LaunchedEffect(recording) {
        while (recording) { delay(1000); elapsed++ }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF07131F)).padding(16.dp)) {
        TopBar(recording, elapsed)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(Modifier.weight(3.2f).fillMaxHeight(), shape = RoundedCornerShape(18.dp)) {
                Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    if (compare) {
                        Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ScopeView(Modifier.weight(1f).fillMaxHeight(), zoom, enhancement, "LIVE")
                            ScopeView(Modifier.weight(1f).fillMaxHeight(), 1f, false, "PREVIOUS")
                        }
                    } else ScopeView(Modifier.fillMaxSize().padding(10.dp), zoom, enhancement, "LIVE")
                    if (recording) RecordBadge(Modifier.align(Alignment.TopStart).padding(18.dp))
                    Text("DEMO IMAGE / NOT FOR DIAGNOSIS", color = Color.White.copy(.55f), fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp))
                }
            }
            SidePanel(Modifier.weight(1.15f).fillMaxHeight(), captured, zoom, enhancement)
        }
        Spacer(Modifier.height(12.dp))
        ControlBar(
            recording = recording,
            onRecord = { recording = !recording; if (!recording) elapsed = 0 },
            onCapture = { captured++ },
            zoom = zoom,
            onZoom = { zoom = it.coerceIn(1f, 2.5f) },
            compare = compare,
            onCompare = { compare = !compare },
            enhancement = enhancement,
            onEnhancement = { enhancement = !enhancement }
        )
    }
}

@Composable
private fun TopBar(recording: Boolean, elapsed: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.MedicalServices, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        Spacer(Modifier.width(10.dp))
        Column { Text("ENDOSCOPE VIEW SYSTEM", fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("G510 UI Demonstration", color = Color(0xFF89A4B5), fontSize = 12.sp) }
        Spacer(Modifier.weight(1f))
        StatusPill("映像入力", "1080p / 30fps", Color(0xFF39D98A))
        Spacer(Modifier.width(10.dp))
        StatusPill(if (recording) "REC" else "STANDBY", formatTime(elapsed), if (recording) Color(0xFFFF4D5A) else Color(0xFF89A4B5))
        Spacer(Modifier.width(12.dp))
        Text("DEMO", color = Color(0xFFFFD166), fontWeight = FontWeight.Black)
    }
}

@Composable
private fun StatusPill(title: String, value: String, color: Color) {
    Row(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF102231)).padding(horizontal = 13.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color)); Spacer(Modifier.width(7.dp)); Text("$title  $value", fontSize = 12.sp)
    }
}

@Composable
private fun ScopeView(modifier: Modifier, zoom: Float, enhanced: Boolean, label: String) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().scale(zoom)) {
            val r = size.minDimension * .43f
            val c = center
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD7C2), Color(0xFFB94A4A), Color(0xFF2E070A)), c, r), r, c)
            drawCircle(Color(0xFFFFE9D9).copy(.25f), r * .72f, c + Offset(-r * .12f, -r * .08f))
            val vessel = if (enhanced) Color(0xFF730A1A) else Color(0xFF9B3944)
            repeat(12) { i ->
                val a = i * 0.52f
                val p1 = c + Offset(kotlin.math.cos(a) * r * .16f, kotlin.math.sin(a) * r * .16f)
                val p2 = c + Offset(kotlin.math.cos(a + .22f) * r * .78f, kotlin.math.sin(a + .22f) * r * .78f)
                drawLine(vessel.copy(.7f), p1, p2, strokeWidth = 2.5f)
            }
            drawCircle(Color.White.copy(.18f), r * .12f, c + Offset(-r * .28f, -r * .32f))
            drawCircle(Color(0xFF32D7E6), r * .24f, c + Offset(r * .25f, r * .12f), style = Stroke(width = 4f))
        }
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).background(Color.Black.copy(.45f), RoundedCornerShape(6.dp)).padding(8.dp, 4.dp))
    }
}

@Composable
private fun RecordBadge(modifier: Modifier) {
    val alpha by rememberInfiniteTransition(label = "blink").animateFloat(1f, .25f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "a")
    Row(modifier.background(Color.Black.copy(.55f), RoundedCornerShape(8.dp)).padding(10.dp, 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(Color.Red.copy(alpha))); Spacer(Modifier.width(7.dp)); Text("REC", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SidePanel(modifier: Modifier, captured: Int, zoom: Float, enhancement: Boolean) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoCard("検査情報", Icons.Default.Person) {
            InfoRow("患者ID", "DEMO-0001"); InfoRow("氏名", "デモ患者"); InfoRow("検査部位", "上部消化管"); InfoRow("担当", "Demo Operator")
        }
        InfoCard("画像ステータス", Icons.Default.MonitorHeart) {
            InfoRow("入力", "READY"); InfoRow("ズーム", String.format("%.1fx", zoom)); InfoRow("画像強調", if (enhancement) "ON" else "OFF"); InfoRow("保存枚数", "$captured 枚")
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF102B2D))) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Info, null, tint = Color(0xFF32D7E6)); Spacer(Modifier.width(8.dp)); Text("デモ表示", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(6.dp)); Text("AI診断・実患者データ・実録画は使用していません。", color = Color(0xFFB9CFD3), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, body: @Composable ColumnScope.() -> Unit) {
    Card { Column(Modifier.fillMaxWidth().padding(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text(title, fontWeight = FontWeight.Bold) }; HorizontalDivider(Modifier.padding(vertical = 9.dp), color = Color.White.copy(.1f)); body() } }
}

@Composable private fun InfoRow(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(label, Modifier.weight(1f), color = Color(0xFF89A4B5), fontSize = 12.sp); Text(value, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) } }

@Composable
private fun ControlBar(recording: Boolean, onRecord: () -> Unit, onCapture: () -> Unit, zoom: Float, onZoom: (Float) -> Unit, compare: Boolean, onCompare: () -> Unit, enhancement: Boolean, onEnhancement: () -> Unit) {
    Card { Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onRecord, colors = ButtonDefaults.buttonColors(containerColor = if (recording) Color(0xFFFF4D5A) else Color(0xFF213E52))) { Icon(if (recording) Icons.Default.Stop else Icons.Default.FiberManualRecord, null); Spacer(Modifier.width(6.dp)); Text(if (recording) "録画停止" else "録画開始") }
        Button(onClick = onCapture) { Icon(Icons.Default.PhotoCamera, null); Spacer(Modifier.width(6.dp)); Text("静止画保存") }
        VerticalDivider(Modifier.height(34.dp), color = Color.White.copy(.18f))
        OutlinedButton(onClick = { onZoom(zoom - .25f) }) { Icon(Icons.Default.ZoomOut, null) }
        Text(String.format("%.1fx", zoom), Modifier.width(50.dp), textAlign = TextAlign.Center)
        OutlinedButton(onClick = { onZoom(zoom + .25f) }) { Icon(Icons.Default.ZoomIn, null) }
        Spacer(Modifier.weight(1f))
        FilterChip(selected = enhancement, onClick = onEnhancement, label = { Text("画像強調") }, leadingIcon = { Icon(Icons.Default.AutoFixHigh, null) })
        FilterChip(selected = compare, onClick = onCompare, label = { Text("比較表示") }, leadingIcon = { Icon(Icons.Default.Compare, null) })
    } }
}

private fun formatTime(seconds: Int) = "%02d:%02d".format(seconds / 60, seconds % 60)
