package com.example.g510englishcafe

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this, "com.google.android.tts")
        setContent { EnglishCafeApp(::speak, ttsReady) }
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            val result = tts?.setLanguage(Locale.US)
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            tts?.setSpeechRate(0.88f)
            tts?.setPitch(1.0f)
        }
    }

    private fun speak(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "english_cafe")
    }

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown(); super.onDestroy()
    }
}

data class Lesson(
    val title: String,
    val teacher: String,
    val japanese: String,
    val target: String,
    val hints: List<String>
)

private val lessons = listOf(
    Lesson("Cafe Order", "Hello! What would you like to drink?", "こんにちは。お飲み物は何になさいますか？", "I'd like a coffee, please.", listOf("I'd like ...", "coffee", "please")),
    Lesson("Self Introduction", "Nice to meet you. What is your name?", "はじめまして。お名前は？", "My name is Eiichi. Nice to meet you.", listOf("My name is ...", "Nice to meet you")),
    Lesson("Travel", "Where would you like to go today?", "今日はどこへ行きたいですか？", "I'd like to go to the station.", listOf("I'd like to go ...", "station")),
    Lesson("Business", "Could you briefly explain your product?", "製品を簡単に説明していただけますか？", "This product provides fast AI processing at the edge.", listOf("This product ...", "AI processing", "at the edge"))
)

@Composable
fun EnglishCafeApp(speak: (String) -> Unit, ttsReady: Boolean) {
    var lessonIndex by remember { mutableIntStateOf(0) }
    var recognized by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("先生の質問を聞いて、英語で答えてください。") }
    var score by remember { mutableIntStateOf(0) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    val lesson = lessons[lessonIndex]
    val context = androidx.compose.ui.platform.LocalContext.current

    fun evaluate(answer: String) {
        val expected = normalize(lesson.target)
        val actual = normalize(answer)
        val expectedWords = expected.split(" ").filter { it.isNotBlank() }.toSet()
        val actualWords = actual.split(" ").filter { it.isNotBlank() }.toSet()
        val overlap = if (expectedWords.isEmpty()) 0.0 else expectedWords.intersect(actualWords).size.toDouble() / expectedWords.size
        score = (overlap * 100).roundToInt().coerceIn(0, 100)
        feedback = when {
            score >= 80 -> "Excellent! とても自然です。"
            score >= 55 -> "Good! もう一度、語尾まで丁寧に言ってみましょう。"
            else -> "Nice try! お手本を聞いて、ゆっくり繰り返しましょう。"
        }
        speak(if (score >= 80) "Excellent! Very good." else "Nice try. Please listen and try again.")
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecognition(context as Activity, recognizer, { recognizer = it }, { listening = it }, {
            recognized = it; evaluate(it)
        }, { feedback = it })
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFFFF6B35), secondary = Color(0xFF14A6A1))) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFFF6FBFF), Color(0xFFFFF3E9))))) {
            Row(Modifier.fillMaxSize().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Card(Modifier.weight(0.34f).fillMaxHeight(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF17324D))) {
                    Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("AI ENGLISH CAFE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Spacer(Modifier.height(24.dp))
                        Box(Modifier.size(170.dp).background(Color(0xFFFFD6B8), CircleShape), contentAlignment = Alignment.Center) {
                            Text("AI", fontSize = 62.sp, fontWeight = FontWeight.Black, color = Color(0xFF17324D))
                        }
                        Spacer(Modifier.height(18.dp))
                        Text("Emma", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("Your English Coach", color = Color(0xFFB9D8EC), fontSize = 15.sp)
                        Spacer(Modifier.weight(1f))
                        AssistChip(onClick = {}, label = { Text(if (ttsReady) "TTS: Ready" else "TTS: Not ready") })
                        Text("Google TTS / Android Speech", color = Color(0xFFB9D8EC), fontSize = 12.sp)
                    }
                }

                Column(Modifier.weight(0.66f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        lessons.forEachIndexed { i, item ->
                            FilterChip(selected = lessonIndex == i, onClick = {
                                lessonIndex = i; recognized = ""; score = 0; feedback = "先生の質問を聞いて、英語で答えてください。"
                            }, label = { Text(item.title) })
                        }
                    }
                    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(22.dp)) {
                            Text("TEACHER", color = Color(0xFF14A6A1), fontWeight = FontWeight.Bold)
                            Text(lesson.teacher, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFF17324D))
                            Spacer(Modifier.height(5.dp))
                            Text(lesson.japanese, fontSize = 16.sp, color = Color.Gray)
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = { speak(lesson.teacher) }) { Text("▶ 質問を聞く") }
                        }
                    }
                    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF8F7))) {
                        Column(Modifier.padding(20.dp)) {
                            Text("YOUR ANSWER", color = Color(0xFF117C78), fontWeight = FontWeight.Bold)
                            Text(if (recognized.isBlank()) "マイクを押して英語で話してください" else recognized,
                                modifier = Modifier.fillMaxWidth(), minLines = 2, fontSize = 24.sp, textAlign = TextAlign.Center, color = Color(0xFF17324D))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                Button(onClick = {
                                    if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.RECORD_AUDIO)
                                    else startRecognition(context as Activity, recognizer, { recognizer = it }, { listening = it }, { recognized = it; evaluate(it) }, { feedback = it })
                                }, enabled = !listening, modifier = Modifier.height(58.dp)) {
                                    Text(if (listening) "Listening..." else "🎤 話す", fontSize = 20.sp)
                                }
                            }
                        }
                    }
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Card(Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(24.dp)) {
                            Column(Modifier.padding(18.dp)) {
                                Text("お手本", fontWeight = FontWeight.Bold, color = Color(0xFFFF6B35))
                                Text(lesson.target, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(10.dp))
                                Text("ヒント: ${lesson.hints.joinToString(" / ")}", color = Color.Gray)
                                Spacer(Modifier.weight(1f))
                                OutlinedButton(onClick = { speak(lesson.target) }) { Text("▶ お手本を聞く") }
                            }
                        }
                        Card(Modifier.width(210.dp).fillMaxHeight(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0E8))) {
                            Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text("SCORE", fontWeight = FontWeight.Bold, color = Color(0xFFFF6B35))
                                Text("$score", fontSize = 54.sp, fontWeight = FontWeight.Black, color = Color(0xFF17324D))
                                Text(feedback, textAlign = TextAlign.Center, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { recognizer?.destroy() }
    }
}

private fun normalize(value: String): String = value.lowercase(Locale.US)
    .replace(Regex("[^a-z0-9' ]"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

private fun startRecognition(
    activity: Activity,
    current: SpeechRecognizer?,
    save: (SpeechRecognizer) -> Unit,
    listening: (Boolean) -> Unit,
    result: (String) -> Unit,
    error: (String) -> Unit
) {
    if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
        error("音声認識サービスが利用できません。Google音声サービスを確認してください。")
        return
    }
    val sr = current ?: SpeechRecognizer.createSpeechRecognizer(activity).also(save)
    sr.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = listening(true)
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() = listening(false)
        override fun onError(code: Int) { listening(false); error("音声認識エラー: $code。もう一度お試しください。") }
        override fun onResults(bundle: Bundle?) {
            listening(false)
            val text = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (text.isNotBlank()) result(text) else error("音声を認識できませんでした。")
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak English")
    }
    listening(true)
    sr.startListening(intent)
}
