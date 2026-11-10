package com.example.busstopvoice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechRecognitionManager(
    context: Context,
    private val onResult: (String) -> Unit,
    private val onStatus: (String) -> Unit,
    private val onRetryNeeded: (Long) -> Unit,
    private val onFatalError: (String) -> Unit
) {
    private val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private var enabled = false
    private var listening = false

    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 700L)
    }

    init {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true; onStatus("音声を待っています") }
            override fun onBeginningOfSpeech() { onStatus("聞き取り中") }
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { listening = false; onStatus("認識中") }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
            override fun onResults(results: Bundle?) {
                listening = false
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val best = texts.firstOrNull().orEmpty()
                if (best.isNotBlank()) onResult(best) else onRetryNeeded(350)
            }
            override fun onError(error: Int) {
                listening = false
                when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> onFatalError("マイク権限がありません")
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> onRetryNeeded(800)
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    SpeechRecognizer.ERROR_CLIENT,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                    SpeechRecognizer.ERROR_NETWORK -> onRetryNeeded(500)
                    else -> onRetryNeeded(700)
                }
            }
        })
    }

    fun start() {
        enabled = true
        if (!listening) runCatching { recognizer.startListening(intent) }.onFailure { onRetryNeeded(700) }
    }

    fun stop() {
        enabled = false
        listening = false
        runCatching { recognizer.cancel() }
    }

    fun restartIfEnabled() { if (enabled && !listening) start() }
    fun release() { enabled = false; recognizer.destroy() }
}
