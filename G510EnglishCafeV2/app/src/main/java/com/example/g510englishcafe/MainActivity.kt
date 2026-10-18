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
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private data class Lesson(val question: String, val ja: String, val target: String, val hints: String)

    private val lessons = listOf(
        Lesson("Hello! What would you like to drink?", "こんにちは。お飲み物は何になさいますか？", "I'd like a coffee, please.", "I'd like ... / coffee / please"),
        Lesson("Nice to meet you. What is your name?", "はじめまして。お名前は？", "My name is Eiichi. Nice to meet you.", "My name is ... / Nice to meet you"),
        Lesson("Where would you like to go today?", "今日はどこへ行きたいですか？", "I'd like to go to the station.", "I'd like to go ... / station"),
        Lesson("Could you briefly explain your product?", "製品を簡単に説明していただけますか？", "This product provides fast AI processing at the edge.", "This product ... / AI processing / at the edge")
    )

    private lateinit var questionText: TextView
    private lateinit var japaneseText: TextView
    private lateinit var targetText: TextView
    private lateinit var hintText: TextView
    private lateinit var recognizedText: TextView
    private lateinit var scoreText: TextView
    private lateinit var feedbackText: TextView
    private lateinit var ttsStatus: TextView
    private lateinit var micButton: Button

    private var lessonIndex = 0
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        setupButtons()
        showLesson(0)
        tts = TextToSpeech(this, this, "com.google.android.tts")
    }

    private fun bindViews() {
        questionText = findViewById(R.id.questionText)
        japaneseText = findViewById(R.id.japaneseText)
        targetText = findViewById(R.id.targetText)
        hintText = findViewById(R.id.hintText)
        recognizedText = findViewById(R.id.recognizedText)
        scoreText = findViewById(R.id.scoreText)
        feedbackText = findViewById(R.id.feedbackText)
        ttsStatus = findViewById(R.id.ttsStatus)
        micButton = findViewById(R.id.micButton)
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.lessonCafe).setOnClickListener { showLesson(0) }
        findViewById<Button>(R.id.lessonIntro).setOnClickListener { showLesson(1) }
        findViewById<Button>(R.id.lessonTravel).setOnClickListener { showLesson(2) }
        findViewById<Button>(R.id.lessonBusiness).setOnClickListener { showLesson(3) }
        findViewById<Button>(R.id.playQuestionButton).setOnClickListener { speak(lessons[lessonIndex].question) }
        findViewById<Button>(R.id.playTargetButton).setOnClickListener { speak(lessons[lessonIndex].target) }
        micButton.setOnClickListener { ensurePermissionAndListen() }
    }

    private fun showLesson(index: Int) {
        lessonIndex = index
        val lesson = lessons[index]
        questionText.text = lesson.question
        japaneseText.text = lesson.ja
        targetText.text = lesson.target
        hintText.text = "ヒント: ${lesson.hints}"
        recognizedText.text = "マイクを押して英語で話してください"
        scoreText.text = "0"
        feedbackText.text = "先生の質問を聞いて、英語で答えてください。"
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val languageResult = tts?.setLanguage(Locale.US)
            ttsReady = languageResult != TextToSpeech.LANG_MISSING_DATA && languageResult != TextToSpeech.LANG_NOT_SUPPORTED
            tts?.setSpeechRate(0.88f)
        }
        ttsStatus.text = if (ttsReady) "TTS: Ready" else "TTS: Not ready"
    }

    private fun speak(text: String) {
        if (!ttsReady) {
            Toast.makeText(this, "Google TTSを確認してください。", Toast.LENGTH_SHORT).show()
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "g510_english")
    }

    private fun ensurePermissionAndListen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO)
        } else startListening()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startListening()
        else feedbackText.text = "マイク権限を許可してください。"
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            feedbackText.text = "音声認識サービスがありません。Google音声サービスを確認してください。"
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { micButton.text = "Listening..."; micButton.isEnabled = false }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { micButton.text = "🎤 話す"; micButton.isEnabled = true }
                override fun onError(error: Int) {
                    micButton.text = "🎤 話す"; micButton.isEnabled = true
                    feedbackText.text = "音声認識エラー: $error。もう一度お試しください。"
                }
                override fun onResults(results: Bundle?) {
                    micButton.text = "🎤 話す"; micButton.isEnabled = true
                    val answer = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    if (answer.isBlank()) feedbackText.text = "音声を認識できませんでした。" else evaluate(answer)
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun evaluate(answer: String) {
        recognizedText.text = answer
        val expectedWords = normalize(lessons[lessonIndex].target).split(" ").filter { it.isNotBlank() }.toSet()
        val actualWords = normalize(answer).split(" ").filter { it.isNotBlank() }.toSet()
        val score = if (expectedWords.isEmpty()) 0 else (expectedWords.intersect(actualWords).size.toDouble() / expectedWords.size * 100).roundToInt()
        scoreText.text = score.coerceIn(0, 100).toString()
        feedbackText.text = when {
            score >= 80 -> "Excellent! とても自然です。"
            score >= 55 -> "Good! 語尾まで丁寧に言ってみましょう。"
            else -> "Nice try! お手本を聞いて、ゆっくり繰り返しましょう。"
        }
        speak(if (score >= 80) "Excellent! Very good." else "Nice try. Please listen and try again.")
    }

    private fun normalize(text: String) = text.lowercase(Locale.US)
        .replace(Regex("[^a-z0-9' ]"), " ")
        .replace(Regex("\\s+"), " ").trim()

    override fun onDestroy() {
        speechRecognizer?.destroy()
        tts?.stop(); tts?.shutdown()
        super.onDestroy()
    }

    companion object { private const val REQUEST_AUDIO = 1001 }
}
