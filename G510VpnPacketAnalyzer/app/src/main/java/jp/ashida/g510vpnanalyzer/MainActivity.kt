package jp.ashida.g510vpnanalyzer

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val vpn = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) startCap()
    }

    private fun startCap() {
        startForegroundService(
            Intent(this, CaptureVpnService::class.java).setAction("START")
        )
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF42D6FF),
                    background = Color(0xFF07111F),
                    surface = Color(0xFF101D2C)
                )
            ) {
                Screen(
                    onStart = {
                        val i = VpnService.prepare(this)
                        if (i != null) vpn.launch(i) else startCap()
                    },
                    onStop = {
                        startService(
                            Intent(this, CaptureVpnService::class.java).setAction("STOP")
                        )
                    }
                )
            }
        }
    }
}

data class Packet(
    val n: Long,
    val version: Int,
    val proto: String,
    val src: String,
    val dst: String,
    val sport: Int?,
    val dport: Int?,
    val len: Int,
    val info: String
)

object CaptureStore {
    val packets = MutableStateFlow<List<Packet>>(emptyList())
    val running = MutableStateFlow(false)
    var bytes = 0L

    fun add(p: Packet) {
        bytes += p.len
        packets.value = (listOf(p) + packets.value).take(500)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen(onStart: () -> Unit, onStop: () -> Unit) {
    val ps by CaptureStore.packets.collectAsState()
    val run by CaptureStore.running.collectAsState()
    var filter by remember { mutableStateOf("") }
    val shown = ps.filter {
        filter.isBlank() ||
                it.proto.contains(filter, true) ||
                it.src.contains(filter) ||
                it.dst.contains(filter) ||
                it.sport?.toString() == filter ||
                it.dport?.toString() == filter
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("G510 VPN Packet Analyzer", fontWeight = FontWeight.Bold) },
                actions = {
                    Text(
                        if (run) "CAPTURING" else "STOPPED",
                        color = if (run) Color(0xFF55E39A) else Color(0xFFFF6B7A)
                    )
                    Spacer(Modifier.width(16.dp))
                }
            )
        }
    ) { p ->
        Column(
            Modifier
                .padding(p)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onStart, enabled = !run) {
                    Text("VPNキャプチャ開始")
                }
                Button(
                    onStop,
                    enabled = run,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB52B3A))
                ) {
                    Text("停止")
                }
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    label = { Text("TCP / UDP / IP / IPまたはPort") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Packets ${ps.size}  Bytes ${CaptureStore.bytes}",
                    modifier = Modifier.padding(12.dp)
                )
            }
            Card {
                Text(
                    "注意: この安全な学習版はTUNから送信パケットを取得しますが、外部へ転送しないため、実行中は端末のインターネット通信が停止します。確認後は必ず停止してください。",
                    Modifier.padding(10.dp),
                    color = Color(0xFFFFCB6B)
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF172638))
                    .padding(7.dp)
            ) {
                H("No", 55)
                H("Proto", 65)
                H("Source", 230)
                H("Destination", 230)
                H("Len", 65)
                H("Info", 0, 1f)
            }
            LazyColumn {
                items(shown) { x ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(if (x.n % 2L == 0L) Color(0xFF0B1725) else Color(0xFF101D2C))
                            .padding(7.dp)
                    ) {
                        C(x.n.toString(), 55)
                        C(x.proto, 65)
                        C("${x.src}${x.sport?.let { ":$it" } ?: ""}", 230)
                        C("${x.dst}${x.dport?.let { ":$it" } ?: ""}", 230)
                        C(x.len.toString(), 65)
                        C(x.info, 0, 1f)
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.H(s: String, w: Int, weightVal: Float = 0f) {
    Text(
        text = s,
        modifier = if (weightVal > 0f) Modifier.weight(weightVal) else Modifier.width(w.dp),
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun RowScope.C(s: String, w: Int, weightVal: Float = 0f) {
    Text(
        text = s,
        modifier = if (weightVal > 0f) Modifier.weight(weightVal) else Modifier.width(w.dp),
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodySmall
    )
}
