package com.example.smosnmos

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MonitorApp(this) }
    }
}

private val Bg = Color(0xFF07131F)
private val Card = Color(0xFF102536)
private val Cyan = Color(0xFF00D9FF)
private val Green = Color(0xFF43E08D)
private val Muted = Color(0xFF8DA6B8)

@Composable
fun MonitorApp(activity: MainActivity) {
    var granted by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var metrics by remember { mutableStateOf(QualityMetrics()) }
    val analyzer = remember { AudioQualityAnalyzer(activity) }
    val scope = rememberCoroutineScope()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, surface = Card, background = Bg)) {
        Column(Modifier.fillMaxSize().background(Bg).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SMOS / NMOS MONITOR", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("Genio 510 / Android  |  16 kHz mono", color = Muted, fontSize = 13.sp)
                }
                StatusPill(if (metrics.speechActive) "SPEECH" else "NO SPEECH", if (metrics.speechActive) Green else Muted)
                Spacer(Modifier.width(12.dp))
                Button(onClick = {
                    if (!granted) permission.launch(Manifest.permission.RECORD_AUDIO)
                    else if (!running) {
                        running = true
                        scope.launch(Dispatchers.Default) { analyzer.run { metrics = it } }
                    } else { analyzer.stop(); running = false }
                }) { Text(if (running) "停止" else if (granted) "測定開始" else "マイク許可") }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ScoreCard("SMOS", "Speech quality proxy", metrics.smos, Cyan, Modifier.weight(1f))
                ScoreCard("NMOS", "Noise quality proxy", metrics.nmos, Green, Modifier.weight(1f))
                Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("入力レベル", "%.1f dBFS".format(metrics.rmsDb), Modifier.weight(1f))
                    MetricCard("推定 SNR", "%.1f dB".format(metrics.snrDb), Modifier.weight(1f))
                    MetricCard("クリッピング", "%.2f %%".format(metrics.clipping), Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(14.dp))
            Waveform(metrics.waveform, Modifier.fillMaxWidth().height(92.dp))
            Text("注: この版のSMOS/NMOSは端末内リアルタイム特徴量による説明可能な相対指標です。正式なP.835/DNSMOS値ではありません。NR ON/OFF比較では同一マイク・距離・音量を使用してください。", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable private fun ScoreCard(title: String, subtitle: String, value: Float, color: Color, modifier: Modifier) {
    Surface(modifier, color = Card, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 21.sp)
            Text(subtitle, color = Muted, fontSize = 11.sp)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Gauge(value, color, Modifier.fillMaxSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%.2f".format(value), color = Color.White, fontSize = 43.sp, fontWeight = FontWeight.Bold)
                    Text("/ 5.00", color = Muted)
                }
            }
            Text(qualityLabel(value), color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable private fun Gauge(value: Float, color: Color, modifier: Modifier) = Canvas(modifier) {
    val stroke = size.minDimension * .065f
    drawArc(Color(0xFF244052), 150f, 240f, false, style = Stroke(stroke, cap = StrokeCap.Round))
    drawArc(color, 150f, 240f * ((value - 1f) / 4f).coerceIn(0f,1f), false, style = Stroke(stroke, cap = StrokeCap.Round))
}

@Composable private fun MetricCard(label: String, value: String, modifier: Modifier) {
    Surface(modifier.fillMaxWidth(), color = Card, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Muted, modifier = Modifier.weight(1f))
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

@Composable private fun StatusPill(text: String, color: Color) {
    Surface(color = color.copy(alpha=.14f), shape = RoundedCornerShape(30.dp)) { Text(text, color=color, modifier=Modifier.padding(horizontal=12.dp, vertical=7.dp), fontWeight=FontWeight.Bold, fontSize=12.sp) }
}

@Composable private fun Waveform(values: List<Float>, modifier: Modifier) = Canvas(modifier.background(Card, RoundedCornerShape(14.dp)).padding(8.dp)) {
    drawLine(Color(0xFF27465A), Offset(0f,size.height/2), Offset(size.width,size.height/2), 1f)
    if(values.size > 1) for(i in 0 until values.lastIndex) {
        val x1=i*size.width/values.lastIndex; val x2=(i+1)*size.width/values.lastIndex
        drawLine(Cyan, Offset(x1,size.height/2-values[i]*size.height*.42f), Offset(x2,size.height/2-values[i+1]*size.height*.42f), 2f)
    }
}
private fun qualityLabel(v: Float)= when { v>=4.2f->"Excellent"; v>=3.4f->"Good"; v>=2.6f->"Fair"; v>=1.8f->"Poor"; else->"Bad" }
