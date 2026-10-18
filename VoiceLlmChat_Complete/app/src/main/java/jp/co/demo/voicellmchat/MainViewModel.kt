package jp.co.demo.voicellmchat

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class ChatMessage(val role: String, val text: String)

class MainViewModel : ViewModel() {
    val messages = mutableStateListOf<ChatMessage>()
    val draft = mutableStateOf("")
    val partial = mutableStateOf("")
    val status = mutableStateOf("設定後、マイクを押して話してください")
    val busy = mutableStateOf(false)
    private var generationJob: Job? = null
    var onSpeak: ((String) -> Unit)? = null

    fun send(endpoint: String, apiKey: String, model: String, input: String = draft.value) {
        val question = input.trim()
        if (question.isEmpty() || busy.value) return
        draft.value = ""
        partial.value = ""
        messages += ChatMessage("user", question)
        busy.value = true
        status.value = "LLMが考えています…"

        generationJob = viewModelScope.launch {
            val answer = StringBuilder()
            try {
                LlmClient().streamChat(endpoint, apiKey, model, messages.toList()) { token ->
                    answer.append(token)
                    partial.value = answer.toString()
                }
                val completed = answer.toString().ifBlank { "LLMから空の応答が返りました。モデル設定を確認してください。" }
                messages += ChatMessage("assistant", completed)
                partial.value = ""
                status.value = "回答を読み上げます"
                onSpeak?.invoke(completed)
            } catch (e: Exception) {
                val errorText = when {
                    e.message?.contains("CLEARTEXT", true) == true -> "HTTP通信が拒否されました。network_security_config.xmlを確認してください。"
                    e.message?.contains("Failed to connect", true) == true -> "LLMサーバーへ接続できません。PCのIP、ポート、Firewallを確認してください。"
                    else -> "通信エラー: ${e.message ?: "不明なエラー"}"
                }
                messages += ChatMessage("assistant", errorText)
                partial.value = ""
                status.value = errorText
            } finally {
                busy.value = false
                if (!status.value.startsWith("通信エラー") && !status.value.contains("接続できません")) {
                    status.value = "マイクを押して続けて話せます"
                }
            }
        }
    }

    fun stop() {
        generationJob?.cancel()
        busy.value = false
        partial.value = ""
        status.value = "生成を停止しました"
    }

    fun clear() {
        generationJob?.cancel()
        messages.clear()
        partial.value = ""
        draft.value = ""
        busy.value = false
        status.value = "会話を消去しました"
    }
}
