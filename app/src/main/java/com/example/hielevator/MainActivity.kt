package com.example.hielevator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var grid: GridLayout
    private lateinit var statusText: TextView
    private lateinit var heardText: TextView
    private var recognizer: SpeechRecognizer? = null
    private lateinit var speechIntent: Intent
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var listening = false
    private var armed = false
    private val handler = Handler(Looper.getMainLooper())
    private val floorButtons = mutableMapOf<Int, Button>()

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListeningSoon(300)
        else showStatus("マイク権限が必要です")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        grid = findViewById(R.id.floorGrid)
        statusText = findViewById(R.id.statusText)
        heardText = findViewById(R.id.heardText)
        createFloorButtons()
        tts = TextToSpeech(this, this)
        setupSpeechRecognizer()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListeningSoon(500)
        } else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun createFloorButtons() {
        for (floor in 1..30) {
            val button = Button(this).apply {
                text = floor.toString()
                textSize = 18f
                setTextColor(Color.WHITE)
                background = getDrawable(R.drawable.floor_button)
                gravity = Gravity.CENTER
                isAllCaps = false
                setOnClickListener { acceptFloor(floor) }
            }
            val size = (resources.displayMetrics.widthPixels - dp(64)) / 5
            val lp = GridLayout.LayoutParams().apply {
                width = size
                height = size
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }
            grid.addView(button, lp)
            floorButtons[floor] = button
        }
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showStatus("この端末では音声認識を利用できません")
            return
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { listening = true; heardText.text = if (armed) "階数を聞いています" else "待機中: Hi エレベーター" }
                override fun onBeginningOfSpeech() { heardText.text = "音声入力中…" }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { listening = false; heardText.text = "認識中…" }
                override fun onError(error: Int) { listening = false; Log.w("HiElevatorSpeech", "onError=$error"); startListeningSoon(if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1200 else 500) }
                override fun onResults(results: Bundle?) { listening = false; handlePhrases(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()) }
                override fun onPartialResults(partialResults: Bundle?) { handlePartial(partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()) }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 700L)
        }
    }

    private fun handlePartial(phrases: List<String>) {
        if (phrases.isEmpty()) return
        heardText.text = "認識: ${phrases.first()}"
        if (!armed && phrases.any { isWakePhrase(it) }) {
            armed = true
            showStatus("はい、何階ですか？")
        }
    }

    private fun handlePhrases(phrases: List<String>) {
        heardText.text = if (phrases.isEmpty()) "認識できませんでした" else "認識: ${phrases.joinToString(" / ")}"
        val joined = phrases.joinToString(" ")
        val hasWake = phrases.any { isWakePhrase(it) }
        val floor = extractFloor(joined)
        when {
            floor != null && (armed || hasWake) -> acceptFloor(floor)
            hasWake -> {
                armed = true
                showStatus("はい、何階ですか？")
                speak("はい、何階ですか")
                startListeningSoon(900)
            }
            armed -> {
                showStatus("1階から30階でお話しください")
                speak("1階から30階でお話しください")
                startListeningSoon(1000)
            }
            else -> startListeningSoon(350)
        }
    }

    private fun isWakePhrase(text: String): Boolean {
        val s = normalize(text)
        return s.contains("hiエレベーター") || s.contains("はいエレベーター") || s.contains("ハイエレベーター") || s.contains("エレベーター")
    }

    private fun extractFloor(text: String): Int? {
        val s = normalize(text)
        val digit = Regex("([1-9]|[12][0-9]|30)(?:階|回)?").find(s)?.groupValues?.get(1)?.toIntOrNull()
        if (digit in 1..30) return digit
        val map = mapOf(
            "一" to 1,"二" to 2,"三" to 3,"四" to 4,"五" to 5,"六" to 6,"七" to 7,"八" to 8,"九" to 9,"十" to 10,
            "十一" to 11,"十二" to 12,"十三" to 13,"十四" to 14,"十五" to 15,"十六" to 16,"十七" to 17,"十八" to 18,"十九" to 19,
            "二十" to 20,"二十一" to 21,"二十二" to 22,"二十三" to 23,"二十四" to 24,"二十五" to 25,"二十六" to 26,"二十七" to 27,"二十八" to 28,"二十九" to 29,"三十" to 30
        )
        return map.entries.sortedByDescending { it.key.length }.firstOrNull { s.contains(it.key + "階") || s == it.key }?.value
    }

    private fun normalize(text: String) = text.lowercase(Locale.JAPAN).replace(" ", "").replace("　", "")

    private fun acceptFloor(floor: Int) {
        armed = false
        floorButtons.values.forEach { it.isSelected = false }
        floorButtons[floor]?.isSelected = true
        showStatus("${floor}階を受け付けました")
        heardText.text = "行き先ボタン: ${floor}F"
        speak("${floor}階を受け付けました。${floor}階へ参ります")
        startListeningSoon(2500)
    }

    private fun speak(message: String) {
        if (!ttsReady) return
        recognizer?.cancel()
        listening = false
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "elevator_msg")
    }

    private fun startListeningSoon(delay: Long) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (!isFinishing && !listening && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                try { recognizer?.startListening(speechIntent) } catch (e: Exception) { Log.e("HiElevatorSpeech", "start failed", e) }
            }
        }, delay)
    }

    private fun showStatus(message: String) { statusText.text = message }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.JAPAN)
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            tts?.setSpeechRate(0.9f)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        recognizer?.cancel(); recognizer?.destroy(); recognizer = null
        tts?.stop(); tts?.shutdown(); tts = null
        super.onDestroy()
    }
}
