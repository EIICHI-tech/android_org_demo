package com.example.busstopvoice

import android.app.Application
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.busstopvoice.model.ConversationMode
import com.example.busstopvoice.model.GuideUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class BusStopViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TimetableRepository(app)
    private val _ui = MutableStateFlow(GuideUiState())
    val ui: StateFlow<GuideUiState> = _ui.asStateFlow()

    private var permissionGranted = false
    private var speaking = false
    private var selectedDestination: String? = null
    private var routeOffset = 0

    private val speech = SpeechRecognitionManager(
        app,
        onResult = ::handleSpeech,
        onStatus = { _ui.value = _ui.value.copy(status = it) },
        onRetryNeeded = { ms -> scheduleListening(ms) },
        onFatalError = { _ui.value = _ui.value.copy(status = "停止", error = it) }
    )

    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(app, TextToSpeech.OnInitListener { result ->
            if (result == TextToSpeech.SUCCESS) {
                tts.language = Locale.JAPAN
                tts.setSpeechRate(0.92f)
                tts.setPitch(1.0f)
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { speaking = true }
                    override fun onError(utteranceId: String?) { speaking = false; scheduleListening(500) }
                    override fun onDone(utteranceId: String?) { speaking = false; scheduleListening(600) }
                })
                _ui.value = _ui.value.copy(status = "待機中")
                if (permissionGranted) scheduleListening(300)
            } else _ui.value = _ui.value.copy(error = "音声合成を初期化できません")
        })
    }

    fun onPermissionResult(granted: Boolean) {
        permissionGranted = granted
        _ui.value = _ui.value.copy(micReady = granted, error = if (granted) null else "マイク権限を許可してください")
        if (granted) scheduleListening(200)
    }

    private fun handleSpeech(raw: String) {
        _ui.value = _ui.value.copy(heardText = raw)
        when (_ui.value.mode) {
            ConversationMode.WAITING_WAKE -> {
                if (isWakeWord(raw)) {
                    _ui.value = _ui.value.copy(
                        mode = ConversationMode.WAITING_DESTINATION,
                        message = "どちらに行きたいですか？",
                        status = "目的地を待っています",
                        route = null
                    )
                    speak("はい、どちらに行きたいですか？")
                } else scheduleListening(250)
            }
            ConversationMode.WAITING_DESTINATION -> {
                when {
                    isCancel(raw) -> reset(true)
                    else -> {
                        val destination = repo.findDestination(raw)
                        if (destination == null) {
                            _ui.value = _ui.value.copy(message = "目的地を確認できませんでした")
                            speak("申し訳ありません。目的地を確認できませんでした。高槻駅、市役所、病院、大阪駅、京都駅、空港など、目的地をお話しください。")
                        } else {
                            selectedDestination = destination
                            routeOffset = 0
                            showRoute(destination, routeOffset)
                        }
                    }
                }
            }
            ConversationMode.SHOWING_RESULT -> handleFollowUp(raw)
        }
    }

    private fun handleFollowUp(raw: String) {
        when {
            raw.contains("次") || raw.contains("その次") -> {
                routeOffset++
                selectedDestination?.let { showRoute(it, routeOffset) }
            }
            raw.contains("もう一度") || raw.contains("繰り返") -> speakCurrent()
            raw.contains("最初") || isCancel(raw) -> reset(true)
            raw.contains("料金") || raw.contains("いくら") -> _ui.value.route?.let { speak("料金は${it.fareYen}円です。") }
            raw.contains("何番") || raw.contains("乗り場") || raw.contains("停留所") -> _ui.value.route?.let { speak("${it.platform}番停留所です。") }
            raw.contains("何分") || raw.contains("時間") -> _ui.value.route?.let { speak("所要時間は約${it.durationMinutes}分です。") }
            else -> {
                val destination = repo.findDestination(raw)
                if (destination != null) {
                    selectedDestination = destination; routeOffset = 0; showRoute(destination, 0)
                } else speak("次のバス、もう一度、料金、乗り場、または終了、とお話しください。")
            }
        }
    }

    private fun showRoute(destination: String, offset: Int) {
        val result = repo.nextRoute(destination, offset)
        if (result == null) {
            routeOffset = 0
            speak("本日の${destination}行きの便は登録されていません。別の目的地をお話しください。")
            _ui.value = _ui.value.copy(mode = ConversationMode.WAITING_DESTINATION, message = "別の目的地をお話しください", route = null)
            return
        }
        val (route, departure) = result
        val now = LocalDateTime.now()
        val minutes = Duration.between(now, departure).toMinutes().coerceAtLeast(0).toInt()
        val hhmm = departure.format(DateTimeFormatter.ofPattern("H時mm分"))
        val dayLabel = if (departure.toLocalDate() == now.toLocalDate()) "" else "明日、"
        _ui.value = _ui.value.copy(
            mode = ConversationMode.SHOWING_RESULT,
            status = "ご案内中",
            message = "${destination}への次のバス",
            route = route,
            departureLabel = departure.format(DateTimeFormatter.ofPattern("HH:mm")),
            minutesUntil = minutes,
            error = null
        )
        speak("${destination}ですね。${route.platform}番停留所から、${dayLabel}${hhmm}発、${route.busName}のバスが出ます。所要時間は約${route.durationMinutes}分、料金は${route.fareYen}円です。発車まであと${minutes}分です。")
    }

    private fun speakCurrent() {
        val route = _ui.value.route ?: return
        speak("${route.platform}番停留所から、${_ui.value.departureLabel}発、${route.busName}です。所要時間は約${route.durationMinutes}分、料金は${route.fareYen}円です。")
    }

    private fun reset(withSpeech: Boolean) {
        selectedDestination = null; routeOffset = 0
        _ui.value = GuideUiState(status = "待機中", micReady = permissionGranted)
        if (withSpeech) speak("案内を終了します。ご利用の際は、ハイ、バス停、とお話しください。") else scheduleListening(300)
    }

    private fun speak(text: String) {
        speech.stop()
        speaking = true
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "guide-${System.nanoTime()}")
    }

    private fun scheduleListening(delayMs: Long) {
        if (!permissionGranted || speaking) return
        viewModelScope.launch {
            delay(delayMs)
            if (permissionGranted && !speaking) speech.start()
        }
    }

    private fun isWakeWord(raw: String): Boolean {
        val s = raw.lowercase().replace(" ", "").replace("　", "")
        return listOf("hibasutei", "hiバス停", "ハイバス停", "はいバス停", "バス停案内").any { s.contains(it) }
    }
    private fun isCancel(raw: String) = listOf("終了", "キャンセル", "やめる", "終わり").any { raw.contains(it) }

    override fun onCleared() { speech.release(); tts.shutdown(); super.onCleared() }
}
