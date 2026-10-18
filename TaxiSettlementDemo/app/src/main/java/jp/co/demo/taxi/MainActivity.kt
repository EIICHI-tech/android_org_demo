package jp.co.demo.taxi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.co.demo.taxi.ui.theme.TaxiTheme
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TaxiTheme { TaxiSettlementApp() } }
    }
}

enum class Screen { HOME, INPUT, RESULT, PAYMENT, QR, RECEIPT }
data class FareBreakdown(val base: Int, val distance: Int, val time: Int, val night: Int, val highway: Int) {
    val total get() = base + distance + time + night + highway
}

@Composable
fun TaxiSettlementApp() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var fare by remember { mutableStateOf(FareBreakdown(500, 3750, 1200, 1000, 1250)) }
    var payment by remember { mutableStateOf("QR決済") }
    Surface(Modifier.fillMaxSize()) {
        when (screen) {
            Screen.HOME -> Home { screen = Screen.INPUT }
            Screen.INPUT -> FareInput(onBack = { screen = Screen.HOME }) { f -> fare = f; screen = Screen.RESULT }
            Screen.RESULT -> Result(fare, { screen = Screen.INPUT }, { screen = Screen.PAYMENT })
            Screen.PAYMENT -> Payment(fare.total, { screen = Screen.RESULT }) { method ->
                payment = method
                screen = if (method == "QR決済") Screen.QR else Screen.RECEIPT
            }
            Screen.QR -> QrPayment(fare.total, { screen = Screen.PAYMENT }, { screen = Screen.RECEIPT })
            Screen.RECEIPT -> Receipt(fare.total, payment) { screen = Screen.HOME }
        }
    }
}

@Composable
private fun Header(title: String, subtitle: String, back: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "戻る") }
        Icon(Icons.Default.LocalTaxi, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
        Spacer(Modifier.width(12.dp))
        Column { Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.LightGray) }
    }
}

@Composable
private fun Home(start: () -> Unit) {
    Row(Modifier.fillMaxSize().padding(32.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            Header("TAXI SETTLEMENT", "次世代タクシー精算デモ")
            Spacer(Modifier.height(32.dp))
            Text("乗車から決済まで、\nスマートに。", fontSize = 42.sp, lineHeight = 50.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(24.dp))
            Button(onClick = start, modifier = Modifier.height(60.dp).width(260.dp)) {
                Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("精算を開始", fontSize = 20.sp)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Metric("本日の売上", "¥452,300", Icons.Default.Payments)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1f)) { Metric("乗車回数", "127回", Icons.Default.LocalTaxi) }
                Box(Modifier.weight(1f)) { Metric("平均単価", "¥3,561", Icons.Default.TrendingUp) }
            }
            Metric("稼働率", "88%", Icons.Default.Speed)
        }
    }
}

@Composable
private fun Metric(label: String, value: String, icon: ImageVector) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF222630))) {
        Row(Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp)); Spacer(Modifier.width(16.dp))
            Column { Text(label, color = Color.LightGray); Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun FareInput(onBack: () -> Unit, calculate: (FareBreakdown) -> Unit) {
    var distance by remember { mutableStateOf("12.5") }; var minutes by remember { mutableStateOf("24") }
    var highway by remember { mutableStateOf("1250") }; var night by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().padding(28.dp).verticalScroll(rememberScrollState())) {
        Header("料金入力", "乗車情報を入力してください", onBack); Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            NumberField("乗車距離", "km", distance, { distance = it }, Modifier.weight(1f))
            NumberField("乗車時間", "分", minutes, { minutes = it }, Modifier.weight(1f))
            NumberField("高速料金", "円", highway, { highway = it }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        Card { Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DarkMode, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text("深夜・早朝割増", fontWeight = FontWeight.Bold); Text("デモ固定割増 ¥1,000", color = Color.LightGray) }
            Switch(checked = night, onCheckedChange = { night = it })
        } }
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            val d = distance.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
            val m = minutes.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val h = highway.toIntOrNull()?.coerceAtLeast(0) ?: 0
            calculate(FareBreakdown(500, (d * 300).toInt(), m * 50, if (night) 1000 else 0, h))
        }, modifier = Modifier.align(Alignment.End).width(260.dp).height(58.dp)) { Text("料金を計算", fontSize = 19.sp) }
    }
}

@Composable
private fun NumberField(label: String, suffix: String, value: String, change: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(value, { if (it.all { c -> c.isDigit() || c == '.' }) change(it) }, label = { Text(label) },
        suffix = { Text(suffix) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = modifier)
}

@Composable
private fun Result(f: FareBreakdown, back: () -> Unit, next: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp)) {
        Header("精算結果", "料金内訳をご確認ください", back); Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Card(Modifier.weight(1.1f)) { Column(Modifier.padding(20.dp)) {
                FareRow("基本料金", f.base); FareRow("距離料金", f.distance); FareRow("時間料金", f.time)
                FareRow("深夜料金", f.night); FareRow("高速料金", f.highway)
            } }
            Card(Modifier.weight(.9f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("お支払い合計", color = Color(0xFF111318)); Text(yen(f.total), color = Color(0xFF111318), fontSize = 48.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(24.dp)); Button(onClick = next, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111318)), modifier = Modifier.fillMaxWidth().height(58.dp)) { Text("支払いへ") }
                }
            }
        }
    }
}

@Composable private fun FareRow(label: String, amount: Int) { Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Text(label, Modifier.weight(1f)); Text(yen(amount), fontWeight = FontWeight.Bold) } }

@Composable
private fun Payment(total: Int, back: () -> Unit, select: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp)) {
        Header("お支払い", "支払方法を選択してください", back); Spacer(Modifier.height(16.dp))
        Text(yen(total), fontSize = 44.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            PaymentCard("クレジット", Icons.Default.CreditCard, Modifier.weight(1f)) { select("クレジット") }
            PaymentCard("QR決済", Icons.Default.QrCode2, Modifier.weight(1f)) { select("QR決済") }
            PaymentCard("現金", Icons.Default.Payments, Modifier.weight(1f)) { select("現金") }
        }
    }
}

@Composable
private fun PaymentCard(title: String, icon: ImageVector, modifier: Modifier, click: () -> Unit) {
    Card(onClick = click, modifier = modifier.height(210.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF222630))) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp)); Spacer(Modifier.height(16.dp)); Text(title, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QrPayment(total: Int, back: () -> Unit, done: () -> Unit) {
    Row(Modifier.fillMaxSize().padding(28.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        Column(Modifier.weight(1f)) { Header("QRコード決済", "スマートフォンで読み取ってください", back); Spacer(Modifier.height(28.dp)); Text(yen(total), fontSize = 46.sp, fontWeight = FontWeight.Black); Text("決済待機中...", color = MaterialTheme.colorScheme.primary) }
        Card(Modifier.weight(1f).fillMaxHeight()) { Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            DummyQr(); Spacer(Modifier.height(18.dp)); Button(onClick = done, modifier = Modifier.width(240.dp).height(54.dp)) { Icon(Icons.Default.CheckCircle, null); Spacer(Modifier.width(8.dp)); Text("デモ決済完了") }
        } }
    }
}

@Composable
private fun DummyQr() {
    val pattern = listOf("1111111010101111111","1000001011101000001","1011101010101011101","1011101000101011101","1011101011101011101","1000001010101000001","1111111010101111111","0000000011100000000","1010111110011010101","0101100011100101110","1110011010111010011","0011100101000111100","1111111011011010101","1000001000110011010","1011101011011110111","1011101001100001100","1011101010111010101","1000001011000101110","1111111010111010011")
    Column(Modifier.background(Color.White, RoundedCornerShape(8.dp)).padding(12.dp)) {
        pattern.forEach { row -> Row { row.forEach { c -> Box(Modifier.size(9.dp).background(if (c == '1') Color.Black else Color.White)) } } }
    }
}

@Composable
private fun Receipt(total: Int, payment: String, restart: () -> Unit) {
    val date = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(Date()) }
    val tx = remember { "TX" + SimpleDateFormat("yyyyMMddHHmmss", Locale.JAPAN).format(Date()) }
    Row(Modifier.fillMaxSize().padding(28.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        Column(Modifier.weight(.8f)) { Header("決済完了", "ご利用ありがとうございました"); Spacer(Modifier.height(22.dp)); Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4ADE80), modifier = Modifier.size(92.dp)); Spacer(Modifier.height(22.dp)); Button(onClick = restart, modifier = Modifier.width(260.dp).height(58.dp)) { Text("新規精算") } }
        Card(Modifier.weight(1.2f).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F5EC))) {
            Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("電子領収書", color = Color.Black, fontSize = 28.sp, fontWeight = FontWeight.Bold); HorizontalDivider(Modifier.padding(vertical = 16.dp), color = Color.Gray)
                ReceiptLine("利用日時", date); ReceiptLine("支払方法", payment); ReceiptLine("取引番号", tx)
                Spacer(Modifier.weight(1f)); Text(yen(total), color = Color.Black, fontSize = 46.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(12.dp)); DummyQr(); Text("デモ用QR", color = Color.Gray)
            }
        }
    }
}

@Composable private fun ReceiptLine(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) { Text(label, Modifier.weight(1f), color = Color.DarkGray); Text(value, color = Color.Black, fontWeight = FontWeight.Bold) } }
private fun yen(value: Int): String = "¥" + NumberFormat.getIntegerInstance(Locale.JAPAN).format(value)
