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
import android.speech.tts.TextToSpeech
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

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var orderTone: ToneGenerator? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textToSpeech = TextToSpeech(this, this)

        orderTone = ToneGenerator(
            AudioManager.STREAM_NOTIFICATION,
            100
        )

        setContent {
            DemoApp(
                onAnnounceOrder = { product ->
                    announceOrder(product)
                }
            )
        }
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            ttsReady = false
            return
        }

        val tts = textToSpeech ?: return

        val result = tts.setLanguage(Locale.JAPANESE)

        ttsReady =
            result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED

        if (ttsReady) {
            tts.setSpeechRate(0.88f)
            tts.setPitch(1.0f)
        }
    }

    private fun announceOrder(product: Product) {
        orderTone?.startTone(
            ToneGenerator.TONE_PROP_ACK,
            250
        )

        Handler(Looper.getMainLooper()).postDelayed({
            if (!ttsReady) {
                return@postDelayed
            }

            val spokenNumber = product.number
                .map { digitToJapanese(it) }
                .joinToString("、")

            val message = buildString {
                append("ご注文ありがとうございます。")
                append("店員さん、")
                append("商品番号、")
                append(spokenNumber)
                append("。")
                append(product.name)
                append("をお願いします。")
            }

            textToSpeech?.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "ORDER_${product.number}"
            )
        }, 350L)
    }

    private fun digitToJapanese(character: Char): String {
        return when (character) {
            '0' -> "ゼロ"
            '1' -> "いち"
            '2' -> "に"
            '3' -> "さん"
            '4' -> "よん"
            '5' -> "ご"
            '6' -> "ろく"
            '7' -> "なな"
            '8' -> "はち"
            '9' -> "きゅう"
            else -> character.toString()
        }
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null

        orderTone?.release()
        orderTone = null

        super.onDestroy()
    }
}

data class Product(
    val number: String,
    val name: String,
    val keywords: List<String>,
    val drawable: String
)

enum class AgeGate {
    WAITING_FOR_FACE,
    CHECK_REQUIRED,
    CONFIRMED
}

enum class VoiceMode {
    STOPPED,
    WAKE,
    ORDER
}

fun loadCatalog(context: Context): List<Product> {
    val list = mutableListOf<Product>()
    try {
        val jsonString = context.assets.open("catalog.json").bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val number = obj.getString("number")
            val name = obj.getString("name")
            val kwArray = obj.getJSONArray("keywords")
            val keywords = mutableListOf<String>()
            for (j in 0 until kwArray.length()) {
                keywords.add(kwArray.getString(j))
            }
            val drawable = obj.getString("drawable")
            list.add(Product(number, name, keywords, drawable))
        }
    } catch (_: Exception) {}
    return list
}

class HiTabakoRecognizer(
    private val context: Context,
    private val products: List<Product>,
    private val onMode: (VoiceMode) -> Unit,
    private val onHeard: (String) -> Unit,
    private val onOrder: (Product) -> Unit
) : RecognitionListener {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val recognizer =
        SpeechRecognizer.createSpeechRecognizer(context)

    private val wakeTone = ToneGenerator(
        AudioManager.STREAM_NOTIFICATION,
        80
    )

    private var enabled = false
    private var mode = VoiceMode.STOPPED
    private var starting = false
    private var destroyed = false

    private val recognitionIntent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "ja-JP"
            )
            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
            )
            putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                5
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                900L
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                650L
            )
        }

    init {
        recognizer.setRecognitionListener(this)
    }

    fun start() {
        if (destroyed) {
            return
        }

        enabled = true
        starting = false
        mode = VoiceMode.WAKE

        onMode(mode)
        onHeard("「Hi Tabako」と話してください")

        listen(150L)
    }

    fun stop() {
        enabled = false
        starting = false
        mode = VoiceMode.STOPPED

        mainHandler.removeCallbacksAndMessages(null)

        onMode(mode)

        try {
            recognizer.cancel()
        } catch (_: Exception) {
        }
    }

    fun destroy() {
        if (destroyed) {
            return
        }

        destroyed = true
        enabled = false
        starting = false

        mainHandler.removeCallbacksAndMessages(null)

        try {
            recognizer.cancel()
        } catch (_: Exception) {
        }

        try {
            recognizer.destroy()
        } catch (_: Exception) {
        }

        wakeTone.release()
    }

    private fun listen(delay: Long = 250L) {
        if (!enabled || destroyed) {
            return
        }

        mainHandler.removeCallbacksAndMessages(null)

        mainHandler.postDelayed({
            if (!enabled || destroyed || starting) {
                return@postDelayed
            }

            try {
                starting = true

                recognizer.cancel()
                recognizer.startListening(recognitionIntent)
            } catch (_: Exception) {
                starting = false
                listen(700L)
            }
        }, delay)
    }

    private fun normalize(text: String): String {
        return text
            .lowercase(Locale.JAPAN)
            .replace(" ", "")
            .replace("　", "")
            .replace("・", "")
            .replace("ー", "")
    }

    private fun isWakeWord(text: String): Boolean {
        val normalizedText = normalize(text)

        val wakeWords = listOf(
            "hitabako",
            "haitabako",
            "heytabako",
            "ハイタバコ",
            "ヘイタバコ",
            "はいタバコ",
            "ハイたばこ",
            "ハイ煙草"
        )

        return wakeWords.any { wakeWord ->
            normalizedText.contains(normalize(wakeWord))
        }
    }

    private fun matchProduct(text: String): Product? {
        val norm = normalize(text)
        for (p in products) {
            if (norm.contains(p.number) || norm.contains(p.number.trimStart('0'))) {
                return p
            }
            if (norm.contains(normalize(p.name))) {
                return p
            }
            if (p.keywords.any { kw -> norm.contains(normalize(kw)) }) {
                return p
            }
        }
        return null
    }

    private fun consume(
        candidates: List<String>,
        partial: Boolean
    ) {
        val heard = candidates
            .firstOrNull()
            .orEmpty()

        if (heard.isNotBlank()) {
            onHeard(heard)
        }

        if (
            mode == VoiceMode.WAKE &&
            candidates.any(::isWakeWord)
        ) {
            try {
                recognizer.cancel()
            } catch (_: Exception) {
            }

            starting = false

            wakeTone.startTone(
                ToneGenerator.TONE_PROP_ACK,
                180
            )

            mode = VoiceMode.ORDER
            onMode(mode)

            onHeard(
                "Hi Tabakoを認識しました。銘柄をお話しください"
            )

            listen(500L)

        } else if (
            mode == VoiceMode.ORDER &&
            candidates.isNotEmpty()
        ) {
            val matched = candidates.firstNotNullOfOrNull { matchProduct(it) }
            if (matched != null) {
                try {
                    recognizer.cancel()
                } catch (_: Exception) {
                }
                starting = false
                mode = VoiceMode.WAKE
                onMode(mode)
                onOrder(matched)
                onHeard("「Hi Tabako」と話してください")
                listen(1500L)
            }
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onError(error: Int) {
        starting = false
        listen(700L)
    }
    override fun onResults(results: Bundle?) {
        starting = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
        consume(matches, false)
        listen(300L)
    }
    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
        if (matches.isNotEmpty()) {
            consume(matches, true)
        }
    }
    override fun onEvent(eventType: Int, params: Bundle?) {}
}

@Composable
fun DemoApp(
    onAnnounceOrder: (Product) -> Unit
) {
    val context = LocalContext.current
    val products = remember { loadCatalog(context) }

    var ageGate by remember { mutableStateOf(AgeGate.WAITING_FOR_FACE) }
    var voiceMode by remember { mutableStateOf(VoiceMode.STOPPED) }
    var lastHeard by remember { mutableStateOf("音声認識を初期化中...") }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var audioPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        cameraPermissionGranted = perms[Manifest.permission.CAMERA] ?: cameraPermissionGranted
        audioPermissionGranted = perms[Manifest.permission.RECORD_AUDIO] ?: audioPermissionGranted
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionGranted || !audioPermissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    // Voice Recognizer setup
    val recognizerRef = remember {
        mutableStateOf<HiTabakoRecognizer?>(null)
    }

    DisposableEffect(audioPermissionGranted) {
        if (audioPermissionGranted) {
            val rec = HiTabakoRecognizer(
                context = context,
                products = products,
                onMode = { voiceMode = it },
                onHeard = { lastHeard = it },
                onOrder = { product ->
                    selectedProduct = product
                    showConfirmDialog = true
                    onAnnounceOrder(product)
                }
            )
            recognizerRef.value = rec
            rec.start()
        }
        onDispose {
            recognizerRef.value?.destroy()
            recognizerRef.value = null
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column: Camera / Age Gate & Status
            Column(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (cameraPermissionGranted) {
                            AndroidView(
                                factory = { ctx ->
                                    PreviewView(ctx).apply {
                                        scaleType = PreviewView.ScaleType.FILL_CENTER
                                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                update = { previewView ->
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }

                                        val faceDetector = FaceDetection.getClient(
                                            FaceDetectorOptions.Builder()
                                                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                                                .build()
                                        )

                                        val analysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()

                                        val executor = Executors.newSingleThreadExecutor()
                                        analysis.setAnalyzer(executor) { imageProxy ->
                                            val mediaImage = imageProxy.image
                                            if (mediaImage != null) {
                                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                                faceDetector.process(image)
                                                    .addOnSuccessListener { faces ->
                                                        if (faces.isNotEmpty()) {
                                                            if (ageGate == AgeGate.WAITING_FOR_FACE) {
                                                                ageGate = AgeGate.CHECK_REQUIRED
                                                            }
                                                        }
                                                    }
                                                    .addOnCompleteListener {
                                                        imageProxy.close()
                                                    }
                                            } else {
                                                imageProxy.close()
                                            }
                                        }

                                        try {
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                context as ComponentActivity,
                                                CameraSelector.DEFAULT_FRONT_CAMERA,
                                                preview,
                                                analysis
                                            )
                                        } catch (_: Exception) {}
                                    }, ContextCompat.getMainExecutor(context))
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("カメラ権限が必要です", color = Color.White)
                            }
                        }

                        // Age gate overlay / badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(8.dp)
                                .background(
                                    color = when (ageGate) {
                                        AgeGate.WAITING_FOR_FACE -> Color.Black.copy(alpha = 0.7f)
                                        AgeGate.CHECK_REQUIRED -> Color(0xFFD32F2F)
                                        AgeGate.CONFIRMED -> Color(0xFF388E3C)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = when (ageGate) {
                                    AgeGate.WAITING_FOR_FACE -> "人物を検出中..."
                                    AgeGate.CHECK_REQUIRED -> "20歳以上確認が必要です (スタッフ確認)"
                                    AgeGate.CONFIRMED -> "年齢確認済み (販売可能)"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        if (ageGate == AgeGate.CHECK_REQUIRED) {
                            Button(
                                onClick = { ageGate = AgeGate.CONFIRMED },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("スタッフ確認完了 (20歳以上)")
                            }
                        }
                    }
                }

                // Voice status card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        color = when (voiceMode) {
                                            VoiceMode.STOPPED -> Color.Gray
                                            VoiceMode.WAKE -> Color(0xFF1976D2)
                                            VoiceMode.ORDER -> Color(0xFFE65100)
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    )
                            )
                            Text(
                                text = when (voiceMode) {
                                    VoiceMode.STOPPED -> "音声認識 停止中"
                                    VoiceMode.WAKE -> "「Hi Tabako」待ち受け中"
                                    VoiceMode.ORDER -> "銘柄音声受付中"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = lastHeard,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Right Column: Product Catalog Grid
            Column(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "たばこ銘柄一覧 (タッチまたは音声で選択)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products) { product ->
                        ProductCard(
                            product = product,
                            onClick = {
                                selectedProduct = product
                                showConfirmDialog = true
                                onAnnounceOrder(product)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showConfirmDialog && selectedProduct != null) {
        val prod = selectedProduct!!
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("ご注文確認: No.${prod.number}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "銘柄: ${prod.name}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = "店員さんに音声アナウンスを送信しました。レジでの年齢確認をお願いいたします。")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        ageGate = AgeGate.WAITING_FOR_FACE
                    }
                ) {
                    Text("閉じる")
                }
            }
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val drawableId = remember(product.drawable) {
        context.resources.getIdentifier(product.drawable, "drawable", context.packageName)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(90.dp)
                    .fillMaxWidth()
                    .background(Color.LightGray, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (drawableId != 0) {
                    Image(
                        painter = painterResource(id = drawableId),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(product.number, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = "${product.number}: ${product.name}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
