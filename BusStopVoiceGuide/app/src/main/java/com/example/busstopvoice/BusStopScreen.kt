package com.example.busstopvoice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.busstopvoice.model.GuideUiState
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val Navy = Color(0xFF07172A)
private val Cyan = Color(0xFF00C6E8)
private val Yellow = Color(0xFFFFD54F)
private val Panel = Color(0xFF102942)

@Composable
fun BusStopScreen(state: GuideUiState) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, background = Navy, surface = Panel)) {
        Box(Modifier.fillMaxSize().background(Navy).padding(24.dp)) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Header(state.status)
                if (state.route == null) WaitingPanel(state, Modifier.weight(1f).fillMaxWidth()) else ResultPanel(state, Modifier.weight(1f).fillMaxWidth())
                state.error?.let { Text(it, color = Color(0xFFFF6B6B), fontSize = 20.sp) }
                Text("認識内容：${state.heardText.ifBlank { "---" }}", color = Color.LightGray, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun Header(status: String) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(1000) } }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("中央バス停　音声案内", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text("BUS STOP VOICE GUIDE", color = Cyan, fontSize = 18.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(now.format(DateTimeFormatter.ofPattern("HH:mm:ss")), color = Yellow, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text(status, color = Cyan, fontSize = 18.sp)
        }
    }
}

@Composable
private fun WaitingPanel(state: GuideUiState, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Card(Modifier.weight(1.5f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (state.mode.name == "WAITING_WAKE") "HI BASUTEI" else "目的地をお話しください", color = Yellow, fontSize = 50.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(18.dp))
                Text(state.message, color = Color.White, fontSize = 30.sp)
                Spacer(Modifier.height(24.dp))
                Text("ボタン操作は不要です", color = Cyan, fontSize = 22.sp)
            }
        }
        Card(Modifier.weight(1f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2238)), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(24.dp)) {
                Text("案内できる目的地", color = Cyan, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                listOf("高槻駅", "市役所", "病院", "大阪駅", "京都駅", "空港").forEach {
                    Text("●  $it", color = Color.White, fontSize = 23.sp, modifier = Modifier.padding(vertical = 7.dp))
                }
            }
        }
    }
}

@Composable
private fun ResultPanel(state: GuideUiState, modifier: Modifier = Modifier) {
    val route = state.route ?: return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(route.destination + " へのご案内", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InfoCard("乗り場", "${route.platform}番", Yellow, Modifier.weight(1f))
            InfoCard("発車時刻", state.departureLabel, Cyan, Modifier.weight(1f))
            InfoCard("発車まで", "あと${state.minutesUntil ?: 0}分", Color(0xFF7CFF8A), Modifier.weight(1f))
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InfoCard("行き先", route.busName, Color.White, Modifier.weight(1.5f))
            InfoCard("所要時間", "約${route.durationMinutes}分", Color.White, Modifier.weight(1f))
            InfoCard("料金", "${route.fareYen}円", Yellow, Modifier.weight(1f))
        }
        Text("続けて『次のバス』『もう一度』『料金』『乗り場』『終了』と話せます", color = Cyan, fontSize = 20.sp)
    }
}

@Composable
private fun InfoCard(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color.LightGray, fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text(value, color = valueColor, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
    }
}
