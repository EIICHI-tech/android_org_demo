package jp.co.kagafei.earmg510

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val vm: EArmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EArmApp(vm) }
    }

    override fun onStop() {
        vm.stopAll("画面離脱のため安全停止しました")
        super.onStop()
    }
}

private val Navy = Color(0xFF071626)
private val Panel = Color(0xFF102B40)
private val Cyan = Color(0xFF00D4FF)
private val Green = Color(0xFF15D69A)
private val Red = Color(0xFFFF3B4D)

@Composable
fun EArmApp(vm: EArmViewModel) {
    val context = LocalContext.current
    val connected by vm.connected.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val active by vm.activeCommand.collectAsStateWithLifecycle()
    var buzzer by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()?.let(vm::voiceCommand)
    }
    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) launchSpeech(context, speechLauncher) }

    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, background = Navy, surface = Panel)) {
        Surface(Modifier.fillMaxSize(), color = Navy) {
            Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("eArm G510 CONTROLLER", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        Text(message, color = if (connected) Green else Color(0xFFFFC857), fontSize = 14.sp)
                    }
                    StatusPill(connected)
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }) { Text("Wi-Fi設定") }
                }

                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ControlPanel("台座回転", "左回転", "a_minus", "右回転", "a_plus", active, vm, Modifier.weight(1f))
                    ControlPanel("上腕", "上げる", "b_minus", "下げる", "b_plus", active, vm, Modifier.weight(1f))
                    ControlPanel("前腕", "上げる", "c_plus", "下げる", "c_minus", active, vm, Modifier.weight(1f))
                    ControlPanel("グリッパー", "開く", "d_minus", "閉じる", "d_plus", active, vm, Modifier.weight(1f))
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                            launchSpeech(context, speechLauncher)
                        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }, modifier = Modifier.weight(1f).height(58.dp)) { Text("音声操作", fontSize = 18.sp) }
                    Button(onClick = { buzzer = !buzzer; vm.buzzer(buzzer) }, modifier = Modifier.weight(1f).height(58.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF39C12))) {
                        Text(if (buzzer) "ブザー OFF" else "ブザー ON", fontSize = 18.sp)
                    }
                    Button(onClick = { vm.stopAll() }, modifier = Modifier.weight(2f).height(58.dp), colors = ButtonDefaults.buttonColors(containerColor = Red)) {
                        Text("緊急停止", fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(connected: Boolean) {
    Row(Modifier.background(if (connected) Color(0xFF0D4D3D) else Color(0xFF563B18), RoundedCornerShape(30.dp)).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(if (connected) Green else Color(0xFFFFC857), RoundedCornerShape(50)))
        Spacer(Modifier.width(8.dp))
        Text(if (connected) "ONLINE" else "OFFLINE", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ControlPanel(title: String, leftLabel: String, leftCmd: String, rightLabel: String, rightCmd: String, active: String?, vm: EArmViewModel, modifier: Modifier = Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(20.dp)).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        HoldButton(leftLabel, leftCmd, active == leftCmd, vm, Modifier.weight(1f).fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        HoldButton(rightLabel, rightCmd, active == rightCmd, vm, Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
private fun HoldButton(label: String, command: String, pressed: Boolean, vm: EArmViewModel, modifier: Modifier = Modifier) {
    Box(modifier.background(if (pressed) Cyan else Color(0xFF1D526D), RoundedCornerShape(16.dp)).pointerInput(command) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            vm.press(command)
            try { waitForUpOrCancellation() } finally { vm.release(command) }
        }
    }, contentAlignment = Alignment.Center) {
        Text(label, color = if (pressed) Navy else Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

private fun launchSpeech(context: android.content.Context, launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.JAPAN.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "例: 左へ、グリッパーを開いて、停止")
    }
    runCatching { launcher.launch(intent) }
}
