package com.ashida.carclimatevoice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceController(
    context: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit,
    private val onListening: (Boolean) -> Unit,
    private val onError: (String) -> Unit
) : RecognitionListener, TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onError("この端末では音声認識を利用できません")
            return
        }
        stop()
        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also { it.setRecognitionListener(this) }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        recognizer?.startListening(intent)
        onListening(true)
    }

    fun stop() {
        recognizer?.stopListening()
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        onListening(false)
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "climate_reply")
    }

    fun release() {
        stop(); tts?.stop(); tts?.shutdown(); tts = null
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.JAPAN
            tts?.setSpeechRate(0.95f)
        }
    }
    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() { onListening(false) }
    override fun onError(error: Int) {
        onListening(false)
        val message = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "音声を認識できませんでした"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "音声が聞こえませんでした"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "マイク権限がありません"
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "音声認識ネットワークエラーです"
            else -> "音声認識エラー: $error"
        }
        onError(message)
    }
    override fun onResults(results: Bundle?) {
        onListening(false)
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onFinal)
            ?: onError("音声を認識できませんでした")
    }
    override fun onPartialResults(partialResults: Bundle?) {
        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onPartial)
    }
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}
