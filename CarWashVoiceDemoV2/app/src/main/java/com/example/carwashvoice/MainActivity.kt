package com.example.carwashvoice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var recognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private val handler = Handler(Looper.getMainLooper())
    private var listening by mutableStateOf(false)
    private var wakeActive by mutableStateOf(false)
    private var status by mutableStateOf("待機中：『Hi GEMINI』と話してください")
    private var selected by mutableStateOf<WashMenu?>(null)
    private var tireWash by mutableStateOf(false)
    private var confirmed by mutableStateOf(false)
    private var recognitionEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer.setRecognitionListener(listener)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        } else handler.postDelayed({ startListening() }, 500)
        setContent { CarWashMenuScreen() }
    }

    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startListening()
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) {
            tts.language = Locale.JAPANESE
            tts.setSpeechRate(0.95f)
        }
    }

    private fun speak(text: String, resumeDelay: Long = 400L) {
        recognitionEnabled = false
        runCatching { recognizer.cancel() }
        listening = false
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "carwash")
        handler.postDelayed({ recognitionEnabled = true; startListening() }, text.length * 95L + 900L + resumeDelay)
    }

    private fun startListening() {
        if (!recognitionEnabled || listening || isFinishing || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 900L)
        }
        listening = true
        runCatching { recognizer.startListening(intent) }.onFailure { listening = false; scheduleRestart() }
    }

    private fun scheduleRestart(delay: Long = 450L) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ startListening() }, delay)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { listening = false }
        override fun onError(error: Int) { listening = false; if (recognitionEnabled) scheduleRestart(if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 900 else 350) }
        override fun onResults(results: Bundle?) {
            listening = false
            val candidates = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            processSpeech(candidates)
            if (recognitionEnabled) scheduleRestart()
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val words = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            if (!wakeActive && words.any(::containsWakeWord)) activateWake()
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun normalize(s: String) = s.lowercase(Locale.JAPANESE)
        .replace(" ", "").replace("　", "")
        .replace("gemini", "ジェミニ")

    private fun containsWakeWord(s: String): Boolean {
        val n = normalize(s)
        return listOf("hiジェミニ", "ハイジェミニ", "はいジェミニ", "ジェミニ").any(n::contains)
    }

    private fun activateWake() {
        if (wakeActive) return
        wakeActive = true
        status = "音声操作受付中：メニュー名を話してください"
        speak("はい。洗車メニューをお選びください")
        handler.postDelayed({ if (wakeActive && selected == null) { wakeActive = false; status = "待機中：『Hi GEMINI』と話してください" } }, 20000L)
    }

    private fun processSpeech(candidates: List<String>) {
        val raw = candidates.joinToString(" ")
        val text = normalize(raw)
        if (containsWakeWord(raw)) {
            activateWake()
            val afterWake = text.substringAfter("ジェミニ", "")
            if (afterWake.isNotBlank()) handleCommand(afterWake)
            return
        }
        if (!wakeActive) { status = "待機中：『Hi GEMINI』と話してください"; return }
        handleCommand(text)
    }

    private fun handleCommand(text: String) {
        when {
            listOf("中止", "キャンセル", "最初から", "リセット").any(text::contains) -> reset()
            confirmed -> { status = "受付完了。『最初から』で戻ります" }
            listOf("タイヤ", "タイヤ洗浄").any(text::contains) -> {
                tireWash = !text.contains("なし") && !text.contains("不要") && !text.contains("外して")
                status = if (tireWash) "タイヤ洗浄を追加しました" else "タイヤ洗浄を外しました"
                speak(status + if (selected != null) "。よろしければ、決定、とお話しください" else "。洗車コースをお選びください")
            }
            listOf("決定", "確定", "これで", "予約", "はい").any(text::contains) && selected != null -> {
                confirmed = true
                status = "洗車予約を受け付けました：合計 ${total()}円"
                speak("${selected!!.title}、${selected!!.sizeLabel}、${if(tireWash) "タイヤ洗浄付き" else "タイヤ洗浄なし"}、合計${total()}円です。洗車予約を受け付けました")
            }
            text.contains("大型") || text.contains("2.5往復") || text.contains("3.5往復") || text.contains("追加") -> {
                if (selected != null) {
                    selected = selected!!.largeVersion()
                    status = "${selected!!.title} ${selected!!.sizeLabel} ${selected!!.price}円を選択"
                    speak(status + "。タイヤ洗浄を追加しますか")
                } else speak("先に洗車コースを選んでください")
            }
            else -> {
                val menu = WashMenu.entries.firstOrNull { item -> item.keywords.any(text::contains) }
                if (menu != null) {
                    selected = menu
                    status = "${menu.title} ${menu.sizeLabel} ${menu.price}円を選択"
                    speak(status + "。タイヤ洗浄は追加300円です。追加する場合は、タイヤ洗浄、とお話しください。決定する場合は、決定、とお話しください")
                } else {
                    status = "聞き取れませんでした：$text"
                    speak("メニュー名をもう一度お話しください")
                }
            }
        }
    }

    private fun selectByTouch(item: WashMenu) {
        wakeActive = true; selected = item; confirmed = false
        status = "${item.title} ${item.sizeLabel} ${item.price}円を選択"
        speak(status + "。決定する場合は、決定、とお話しください")
    }

    private fun total() = (selected?.price ?: 0) + if (tireWash) 300 else 0
    private fun reset() {
        selected = null; tireWash = false; confirmed = false; wakeActive = false
        status = "待機中：『Hi GEMINI』と話してください"
        speak("最初の画面に戻りました。ハイ、ジェミニ、と話しかけてください")
    }

    @Composable private fun CarWashMenuScreen() {
        val navy = Color(0xFF071A2A); val cyan = Color(0xFF00D4FF)
        MaterialTheme {
            Column(Modifier.fillMaxSize().background(navy).padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("VOICE CAR WASH", color=cyan, fontSize=18.sp, fontWeight=FontWeight.Bold)
                        Text("洗車コースを選択", color=Color.White, fontSize=28.sp, fontWeight=FontWeight.Bold)
                    }
                    Surface(color=if(wakeActive) Color(0xFF0B5A45) else Color(0xFF173A50), shape=RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(horizontal=20.dp, vertical=8.dp), horizontalAlignment=Alignment.CenterHorizontally) {
                            Text(if(wakeActive) "● 音声受付中" else "○ 音声待機中", color=if(wakeActive) Color(0xFF6DFFBD) else Color(0xFF9DDDEC), fontWeight=FontWeight.Bold)
                            Text(if(wakeActive) "メニュー名を話してください" else "Hi GEMINI で開始", color=Color.White, fontSize=13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxSize(), horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(3f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        WashMenu.entries.chunked(2).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                row.forEach { item -> MenuCard(item, Modifier.weight(1f), selected == item) }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    Column(Modifier.weight(1.15f).fillMaxHeight(), verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        Surface(color=Color(0xFF102F46), shape=RoundedCornerShape(14.dp), modifier=Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Text("選択内容", color=Color(0xFF9DDDEC), fontSize=15.sp)
                                Text(selected?.title ?: "未選択", color=Color.White, fontSize=20.sp, fontWeight=FontWeight.Bold)
                                Text(selected?.sizeLabel ?: "", color=Color.White)
                                HorizontalDivider(Modifier.padding(vertical=8.dp), color=Color(0xFF31566A))
                                Text("タイヤ洗浄", color=Color.White)
                                Button(onClick={ tireWash=!tireWash }, colors=ButtonDefaults.buttonColors(containerColor=if(tireWash) Color(0xFF18A86B) else Color(0xFF3B5361)), modifier=Modifier.fillMaxWidth()) { Text(if(tireWash) "追加済み +300円" else "追加する +300円") }
                                Text("合計 ${total()}円", color=Color(0xFFFFD166), fontSize=26.sp, fontWeight=FontWeight.Bold, modifier=Modifier.padding(top=8.dp))
                            }
                        }
                        Surface(color=Color(0xFF102F46), shape=RoundedCornerShape(14.dp), modifier=Modifier.fillMaxWidth().weight(1f)) {
                            Column(Modifier.padding(14.dp), verticalArrangement=Arrangement.Center) {
                                Text(status, color=Color.White, fontSize=16.sp, fontWeight=FontWeight.Bold)
                                Text("例：\n・Hi GEMINI\n・ティアラコート\n・大型へ変更\n・タイヤ洗浄\n・決定\n・キャンセル", color=Color(0xFF9DDDEC), fontSize=14.sp, modifier=Modifier.padding(top=10.dp))
                            }
                        }
                        Button(onClick={ if(selected!=null){ confirmed=true; status="洗車予約を受け付けました：合計 ${total()}円"; speak(status) } }, enabled=selected!=null && !confirmed, modifier=Modifier.fillMaxWidth().height(50.dp), colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFFFA000))) { Text(if(confirmed) "受付完了" else "洗車予約を決定", color=Color.Black, fontWeight=FontWeight.Bold) }
                        OutlinedButton(onClick={ reset() }, modifier=Modifier.fillMaxWidth()) { Text("中止・最初から", color=Color.White) }
                    }
                }
            }
        }
    }

    @Composable private fun MenuCard(item: WashMenu, modifier: Modifier, isSelected: Boolean) {
        val bg = Color(item.color)
        Surface(color=bg, shape=RoundedCornerShape(12.dp), border=BorderStroke(if(isSelected) 4.dp else 1.dp, if(isSelected) Color.Yellow else Color.White.copy(alpha=.3f)), modifier=modifier.height(125.dp).clickable { selectByTouch(item) }) {
            Column(Modifier.padding(12.dp), verticalArrangement=Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(item.title, color=Color.White, fontSize=19.sp, fontWeight=FontWeight.Bold)
                    if(item.premium) Text("PREMIUM", color=Color.Yellow, fontSize=10.sp, fontWeight=FontWeight.Bold)
                }
                Text(item.description, color=Color.White, fontSize=11.sp, maxLines=1)
                Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.Bottom, horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(item.sizeLabel, color=Color.White, fontSize=12.sp)
                    Text("${item.price}円", color=Color.White, fontSize=24.sp, fontWeight=FontWeight.Black)
                }
            }
        }
    }

    override fun onDestroy() { recognitionEnabled=false; handler.removeCallbacksAndMessages(null); recognizer.destroy(); tts.shutdown(); super.onDestroy() }
}

enum class WashMenu(val title:String, val description:String, val sizeLabel:String, val price:Int, val color:Long, val premium:Boolean=false, val keywords:List<String>) {
    TIARA("ティアラコート", "最上級の輝き・ガラス系コート", "2.5往復", 1800, 0xFF9B2F2F, true, listOf("ティアラ", "ガラスコート")),
    TIARA_L("ティアラコート", "泡・ジェット・下部洗浄付き", "3.5往復", 2000, 0xFFB23A2B, true, listOf("ティアラ大型")),
    SILKY("シルキーコート", "シルクのような滑らかな仕上がり", "1.5往復", 1000, 0xFF4F3B32, false, listOf("シルキー")),
    SILKY_L("シルキーコート", "泡・ジェット・下部洗浄付き", "2.5往復", 1200, 0xFF5B4634, false, listOf("シルキー大型")),
    WAX("ワックス", "手軽にワックス効果", "0.5往復", 600, 0xFF56305F, false, listOf("ワックス")),
    WAX_L("ワックス", "泡・ジェット・下部洗浄付き", "1.5往復", 800, 0xFF67355C, false, listOf("ワックス大型")),
    DEALER("ディーラーコート車対応洗車", "コーティング車におすすめ", "1.5往復", 500, 0xFF15518A, false, listOf("ディーラー", "コート車")),
    DEALER_L("ディーラーコート車対応洗車", "泡・ジェット・下部洗浄付き", "2.5往復", 700, 0xFF17649B, false, listOf("ディーラー大型")),
    SHAMPOO("シャンプー", "汚れだけを落としたい", "0.5往復", 400, 0xFF075A89, false, listOf("シャンプー")),
    SHAMPOO_L("シャンプー", "泡・ジェット・下部洗浄付き", "1.5往復", 600, 0xFF08709B, false, listOf("シャンプー大型"));

    fun largeVersion(): WashMenu = when(this) {
        TIARA -> TIARA_L; SILKY -> SILKY_L; WAX -> WAX_L; DEALER -> DEALER_L; SHAMPOO -> SHAMPOO_L
        else -> this
    }
}
