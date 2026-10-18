package jp.co.kagafei.earmg510

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class EArmViewModel : ViewModel() {
    private val client = EArmClient()
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()
    private val _message = MutableStateFlow("Wi-Fi eArm に接続してください")
    val message: StateFlow<String> = _message.asStateFlow()
    private val _activeCommand = MutableStateFlow<String?>(null)
    val activeCommand: StateFlow<String?> = _activeCommand.asStateFlow()
    private var monitorJob: Job? = null

    init { startMonitor() }

    fun startMonitor() {
        if (monitorJob?.isActive == true) return
        monitorJob = viewModelScope.launch {
            while (isActive) {
                val ok = client.ping()
                _connected.value = ok
                _message.value = if (ok) "eArm 接続済み" else "未接続: Wi-Fi設定で eArm を選択"
                delay(3000)
            }
        }
    }

    fun press(command: String) {
        _activeCommand.value = command
        viewModelScope.launch {
            client.send("$command=1").onFailure {
                _connected.value = false
                _message.value = "送信エラー: ${it.message ?: "接続を確認"}"
            }
        }
    }

    fun release(command: String) {
        if (_activeCommand.value == command) _activeCommand.value = null
        viewModelScope.launch { client.send("$command=0") }
    }

    fun stopAll(reason: String = "緊急停止しました") {
        _activeCommand.value = null
        viewModelScope.launch {
            client.stopAll()
            _message.value = reason
        }
    }

    fun buzzer(on: Boolean) {
        viewModelScope.launch { client.send("buzzer=${if (on) "on" else "off"}") }
    }

    fun voiceCommand(text: String) {
        val command = when {
            "左" in text -> "a_minus"
            "右" in text -> "a_plus"
            ("上腕" in text || "大アーム" in text) && ("上" in text || "上げ" in text) -> "b_minus"
            ("上腕" in text || "大アーム" in text) && ("下" in text || "下げ" in text) -> "b_plus"
            ("前腕" in text || "小アーム" in text) && ("上" in text || "上げ" in text) -> "c_plus"
            ("前腕" in text || "小アーム" in text) && ("下" in text || "下げ" in text) -> "c_minus"
            "開" in text || "離" in text -> "d_minus"
            "閉" in text || "掴" in text || "つか" in text -> "d_plus"
            "停止" in text || "止ま" in text -> null
            else -> "UNKNOWN"
        }
        if (command == null) stopAll("音声で停止しました")
        else if (command == "UNKNOWN") _message.value = "認識しましたが操作を特定できません: $text"
        else viewModelScope.launch {
            press(command)
            delay(450)
            release(command)
            _message.value = "音声操作: $text"
        }
    }

    override fun onCleared() {
        // ViewModel破棄時はMainActivity.onStop側でも停止を送る
        super.onCleared()
    }
}
