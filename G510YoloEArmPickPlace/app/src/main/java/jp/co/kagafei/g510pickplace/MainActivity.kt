package jp.co.kagafei.g510pickplace

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.concurrent.Executors

private const val CAMERA_TAG = "G510PickPlaceCamera"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) { App() }
        }
    }
}

@Composable
fun App(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }

    var overlay by remember { mutableStateOf<OverlayView?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            overlay?.detections = vm.detections
            overlay?.selected = vm.selected
            kotlinx.coroutines.delay(100)
        }
    }

    Row(
        Modifier.fillMaxSize().background(Color(0xFF06131F)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.weight(1f).fillMaxHeight()) {
            if (granted) {
                CameraPanel(
                    onBitmap = vm::analyze,
                    onOverlay = { overlay = it },
                    onSelect = vm::select
                )
            } else {
                Text(
                    "カメラ権限が必要です",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Surface(
                color = Color.Black.copy(alpha = .55f),
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            ) {
                Text(
                    "検出 ${vm.detections.size} / ${vm.inferenceMs} ms",
                    color = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        Column(
            Modifier.width(330.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("G510 D2 YOLO Pick & Place", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Status("カメラ/AI", if (vm.detections.isNotEmpty()) "RUN" else "WAIT")
            Status("eArm Wi-Fi", if (vm.armOnline) "ONLINE" else "OFFLINE")
            Status("状態", vm.state.name)
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text("選択ターゲット", style = MaterialTheme.typography.titleMedium)
                    Text(vm.selected?.let { "${it.label} ${(it.score * 100).toInt()}%" } ?: "画面上の検出枠をタッチ")
                }
            }
            Card { Text(vm.message, modifier = Modifier.padding(12.dp)) }
            Button(
                onClick = vm::execute,
                enabled = vm.selected != null && vm.armOnline && vm.state != RobotState.APPROACH,
                modifier = Modifier.fillMaxWidth().height(58.dp)
            ) { Text("自動 Pick & Place 実行") }
            Button(
                onClick = vm::emergencyStop,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) { Text("緊急停止", style = MaterialTheme.typography.titleLarge) }
            Text(
                "注意: 実機キャリブレーション用のオープンループPoCです。最初は無負荷・低速・短時間で確認してください。",
                color = Color(0xFFFFCC80)
            )
        }
    }
}

@Composable
private fun Status(k: String, v: String) {
    Card {
        Row(Modifier.fillMaxWidth().padding(10.dp)) {
            Text(k)
            Spacer(Modifier.weight(1f))
            Text(v, color = if (v == "ONLINE" || v == "RUN") Color.Green else Color.Yellow)
        }
    }
}

@Composable
private fun CameraPanel(
    onBitmap: (Bitmap) -> Unit,
    onOverlay: (OverlayView) -> Unit,
    onSelect: (Detection) -> Unit
) {
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    AndroidView(
        factory = { ctx ->
            val frame = android.widget.FrameLayout(ctx)
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val overlay = OverlayView(ctx).apply { this.onSelect = onSelect }
            onOverlay(overlay)
            frame.addView(previewView)
            frame.addView(overlay)

            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                try {
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setTargetResolution(android.util.Size(640, 480))
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    var frameCount = 0
                    analysis.setAnalyzer(executor) { image ->
                        try {
                            if (frameCount++ % 3 == 0) onBitmap(image.toBitmapSafe())
                        } catch (e: Exception) {
                            Log.e(CAMERA_TAG, "Frame analysis failed", e)
                        } finally {
                            image.close()
                        }
                    }

                    provider.unbindAll()
                    // セルフオーダー版と同じ考え方で、G510で存在するFRONTを優先する。
                    val selector = if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    provider.bindToLifecycle(context as ComponentActivity, selector, preview, analysis)
                    Log.i(CAMERA_TAG, "Camera bound successfully")
                } catch (e: Exception) {
                    // Future.get()の例外を未処理にせず、アプリの即時終了を防ぐ。
                    Log.e(CAMERA_TAG, "Camera initialization failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            frame
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun ImageProxy.toBitmapSafe(): Bitmap {
    val plane = planes[0]
    val buffer = plane.buffer
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val rowPadding = rowStride - pixelStride * width
    val padded = Bitmap.createBitmap(
        width + rowPadding / pixelStride,
        height,
        Bitmap.Config.ARGB_8888
    )
    padded.copyPixelsFromBuffer(buffer)
    return Bitmap.createBitmap(padded, 0, 0, width, height).also {
        if (it !== padded) padded.recycle()
    }
}
