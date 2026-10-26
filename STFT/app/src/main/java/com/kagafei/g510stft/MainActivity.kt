package com.kagafei.g510stft

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { StftScreen() } }
    }
}

@Composable
private fun StftScreen(vm: StftViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> granted = ok; if (ok) vm.start() }

    DisposableEffect(Unit) { onDispose { vm.stop() } }

    Column(Modifier.fillMaxSize().background(Color(0xFF071018)).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("G510 REAL-TIME STFT", color = Color(0xFF66E5FF), style = MaterialTheme.typography.headlineSmall)
                Text("16 kHz / 1024 FFT / Hann / 0-8 kHz", color = Color.LightGray)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("LEVEL ${state.levelDb.roundToInt()} dBFS") })
                Button(onClick = { if (state.running) vm.stop() else if (granted) vm.start() else launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text(if (state.running) "STOP" else "START")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Spectrogram(state.history, Modifier.weight(1f).fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Spectrum(state.spectrum, Modifier.height(130.dp).fillMaxWidth())
        if (state.error != null) Text(state.error!!, color = Color.Red)
    }
}

@Composable
private fun Spectrogram(history: List<FloatArray>, modifier: Modifier) {
    Canvas(modifier.background(Color.Black)) {
        if (history.isEmpty()) return@Canvas
        val cw = size.width / history.size
        val bins = history.first().size
        val ch = size.height / bins
        history.forEachIndexed { x, col ->
            col.forEachIndexed { y, db ->
                drawRect(heatColor(db), Offset(x*cw, size.height-(y+1)*ch), Size(cw+1, ch+1))
            }
        }
        for (khz in 0..8 step 2) {
            val y = size.height * (1f-khz/8f)
            drawLine(Color(0x3344FFFF), Offset(0f,y), Offset(size.width,y), 1f)
        }
    }
}

@Composable
private fun Spectrum(spectrum: FloatArray, modifier: Modifier) {
    Canvas(modifier.background(Color(0xFF020608))) {
        if (spectrum.isEmpty()) return@Canvas
        val path = androidx.compose.ui.graphics.Path()
        spectrum.forEachIndexed { i, db ->
            val x = i.toFloat()/(spectrum.size-1).coerceAtLeast(1)*size.width
            val y = size.height*(1f-((db+90f)/90f).coerceIn(0f,1f))
            if (i==0) path.moveTo(x,y) else path.lineTo(x,y)
        }
        drawPath(path, Color(0xFF00E5FF), style=androidx.compose.ui.graphics.drawscope.Stroke(2f))
    }
}

private fun heatColor(db: Float): Color {
    val t=((db+90f)/90f).coerceIn(0f,1f)
    return when {
        t<0.25f -> Color(0f,0f,t*3.2f)
        t<0.5f -> Color(0f,(t-.25f)*4f,1f)
        t<0.75f -> Color((t-.5f)*4f,1f,1f-(t-.5f)*4f)
        else -> Color(1f,1f-(t-.75f)*4f,0f)
    }
}
