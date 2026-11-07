package com.ashida.g510tobacco

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.json.JSONArray
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DemoApp() }
    }
}

data class Product(val number: String, val name: String, val keywords: List<String>, val drawable: String)
enum class AgeGate { WAITING_FOR_FACE, CHECK_REQUIRED, CONFIRMED }
enum class VoiceMode { STOPPED, WAKE, ORDER }

class HiTabakoRecognizer(
    private val context: Context,
    private val onMode: (VoiceMode) -> Unit,
    private val onHeard: (String) -> Unit,
    private val onOrder: (String) -> Unit
) : RecognitionListener {
    private val main = Handler(Looper.getMainLooper())
    private val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
    private var enabled = false
    private var mode = VoiceMode.STOPPED
    private var starting = false

    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 900L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 650L)
    }

    init { recognizer.setRecognitionListener(this) }

    fun start() {
        enabled = true
        mode = VoiceMode.WAKE
        onMode(mode)
        listen(150)
    }

    fun stop() {
        enabled = false
        starting = false
        mode = VoiceMode.STOPPED
        onMode(mode)
        recognizer.cancel()
    }

    fun destroy() {
        enabled = false
        recognizer.destroy()
        tone.release()
        main.removeCallbacksAndMessages(null)
    }

    private fun listen(delay: Long = 250L) {
        if (!enabled) return
        main.removeCallbacksAndMessages(null)
        main.postDelayed({
            if (!enabled || starting) return@postDelayed
            try {
                starting = true
                recognizer.cancel()
                recognizer.startListening(intent)
            } catch (_: Exception) {
                starting = false
                listen(700)
            }
        }, delay)
    }

    private fun normalize(s: String) = s.lowercase(Locale.JAPAN)
        .replace(" ", "").replace("　", "")
        .replace("・", "").replace("ー", "")

    private fun isWakeWord(s: String): Boolean {
        val t = normalize(s)
        return listOf(
            "hitabako", "haitabako", "heytabako",
            "ハイタバコ", "ヘイタバコ", "はいタバコ", "ハイたばこ"
        ).any { t.contains(normalize(it)) }
    }

    private fun consume(candidates: List<String>, partial: Boolean) {
        val heard = candidates.firstOrNull().orEmpty()
        if (heard.isNotBlank()) onHeard(heard)
        if (mode == VoiceMode.WAKE && candidates.any(::isWakeWord)) {
            recognizer.cancel()
            starting = false
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 180)
            mode = VoiceMode.ORDER
            onMode(mode)
            onHeard("Hi Tabakoを認識しました。銘柄をお話しください")
            listen(500)
        } else if (mode == VoiceMode.ORDER && !partial && heard.isNotBlank()) {
            onOrder(heard)
            mode = VoiceMode.WAKE
            onMode(mode)
            listen(1200)
        }
    }

    override fun onReadyForSpeech(params: Bundle?) { starting = false }
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() { starting = false }
    override fun onError(error: Int) { starting = false; listen(if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 900 else 350) }
    override fun onResults(results: Bundle?) {
        starting = false
        consume(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty(), false)
    }
    override fun onPartialResults(partialResults: Bundle?) {
        consume(partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty(), true)
    }
    override fun onEvent(eventType: Int, params: Bundle?) {}
}

@Composable
fun DemoApp() {
    val context = LocalContext.current
    val products = remember { loadCatalog(context) }
    var selected by remember { mutableStateOf<Product?>(null) }
    var transcript by remember { mutableStateOf("「Hi Tabako」と話してください") }
    var voiceMode by remember { mutableStateOf(VoiceMode.STOPPED) }
    var micGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    var faceFound by remember { mutableStateOf(false) }
    var ageGate by remember { mutableStateOf(AgeGate.WAITING_FOR_FACE) }
    var showDialog by remember { mutableStateOf(false) }

    val recognizer = remember {
        HiTabakoRecognizer(
            context = context,
            onMode = { voiceMode = it },
            onHeard = { transcript = it },
            onOrder = { text ->
                transcript = text
                selected = bestMatch(text, products)
                if (selected == null) transcript = "$text（該当する銘柄が見つかりません。もう一度 Hi Tabako と話してください）"
            }
        )
    }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        micGranted = result[Manifest.permission.RECORD_AUDIO] == true || ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (micGranted) recognizer.start()
    }

    LaunchedEffect(Unit) {
        permissions.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        if (micGranted) recognizer.start()
    }
    DisposableEffect(Unit) { onDispose { recognizer.destroy() } }

    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF0B6E4F))) {
        Row(Modifier.fillMaxSize().background(Color(0xFFF3F5F4)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(0.34f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("年齢確認", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                CameraPanel(Modifier.fillMaxWidth().weight(1f)) { found ->
                    faceFound = found
                    if (found && ageGate == AgeGate.WAITING_FOR_FACE) ageGate = AgeGate.CHECK_REQUIRED
                }
                Card(colors = CardDefaults.cardColors(containerColor = if (ageGate == AgeGate.CONFIRMED) Color(0xFFDDF5E8) else Color(0xFFFFE9C7))) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(when (ageGate) {
                            AgeGate.WAITING_FOR_FACE -> "カメラに顔を映してください"
                            AgeGate.CHECK_REQUIRED -> "年齢確認が必要です"
                            AgeGate.CONFIRMED -> "年齢確認済み"
                        }, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("AIの見た目年齢だけで販売可否を決めません。店舗スタッフが公的証明書等で確認してください。", fontSize = 13.sp)
                        Button(onClick = { showDialog = true }, enabled = faceFound && ageGate != AgeGate.CONFIRMED, modifier = Modifier.fillMaxWidth()) { Text("年齢確認を行う") }
                        OutlinedButton(onClick = { ageGate = AgeGate.WAITING_FOR_FACE; faceFound = false }, modifier = Modifier.fillMaxWidth()) { Text("リセット") }
                    }
                }
            }
            Column(Modifier.weight(0.66f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Hi Tabako セルフオーダー", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(when (voiceMode) {
                            VoiceMode.WAKE -> "● 待機中：『Hi Tabako』と話してください"
                            VoiceMode.ORDER -> "● 注文受付中：銘柄を話してください"
                            VoiceMode.STOPPED -> if (micGranted) "音声認識を開始してください" else "マイク権限が必要です"
                        }, color = if (voiceMode == VoiceMode.ORDER) Color(0xFFD84315) else Color(0xFF0B6E4F), fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { if (voiceMode == VoiceMode.STOPPED) recognizer.start() else recognizer.stop() }) {
                        Text(if (voiceMode == VoiceMode.STOPPED) "🎤 開始" else "■ 停止", fontSize = 17.sp)
                    }
                }
                Text(transcript, modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(8.dp)).padding(10.dp))
                selected?.let { p ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2EE))) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProductImage(p, Modifier.size(80.dp, 108.dp)); Spacer(Modifier.width(16.dp))
                            Column {
                                Text(p.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Text("店員さんに番号 ${p.number} をお伝えください", fontSize = 23.sp, color = Color(0xFF9B1C31), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(products) { p ->
                        Card(Modifier.clickable { selected = p; transcript = p.name }) {
                            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                ProductImage(p, Modifier.height(125.dp).fillMaxWidth())
                                Text("No.${p.number}", fontWeight = FontWeight.Bold)
                                Text(p.name, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
    }
    if (showDialog) AlertDialog(
        onDismissRequest = { showDialog = false },
        title = { Text("スタッフによる年齢確認") },
        text = { Text("公的証明書などで20歳以上であることを確認しましたか？") },
        confirmButton = { Button(onClick = { ageGate = AgeGate.CONFIRMED; showDialog = false }) { Text("確認済みにする") } },
        dismissButton = { TextButton(onClick = { showDialog = false }) { Text("キャンセル") } }
    )
}

@Composable
fun ProductImage(p: Product, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val id = context.resources.getIdentifier(p.drawable, "drawable", context.packageName)
    Box(modifier.background(Color.White, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
        Image(painterResource(id), contentDescription = "${p.name}のAI生成デモ画像", modifier = Modifier.fillMaxSize().padding(4.dp), contentScale = ContentScale.Fit)
    }
}

@Composable
fun CameraPanel(modifier: Modifier, onFaceChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    AndroidView(factory = { ctx -> PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }, modifier = modifier, update = { view ->
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return@AndroidView
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
            val detector = FaceDetection.getClient(FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).build())
            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { proxy ->
                val media = proxy.image
                if (media == null) { proxy.close(); return@setAnalyzer }
                detector.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                    .addOnSuccessListener { onFaceChanged(it.isNotEmpty()) }
                    .addOnCompleteListener { proxy.close() }
            }
            try { provider.unbindAll(); provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis) } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
    })
}

fun loadCatalog(context: Context): List<Product> {
    val a = JSONArray(context.assets.open("catalog.json").bufferedReader().readText())
    return (0 until a.length()).map { i ->
        val o = a.getJSONObject(i); val ks = o.getJSONArray("keywords")
        Product(o.getString("number"), o.getString("name"), (0 until ks.length()).map { ks.getString(it) }, o.getString("drawable"))
    }
}

fun bestMatch(text: String, products: List<Product>): Product? {
    val normalized = text.lowercase(Locale.JAPAN).replace("・", "").replace(" ", "").replace("　", "")
    return products.firstOrNull { p -> p.keywords.any { k -> normalized.contains(k.lowercase(Locale.JAPAN).replace(" ", "").replace("・", "")) } }
        ?: products.firstOrNull { normalized.contains(it.number) }
}
