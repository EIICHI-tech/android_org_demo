package com.ashida.carclimatevoice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlin.math.roundToInt

class ClimateViewModel : ViewModel() {
    var state by mutableStateOf(ClimateState())
        private set

    fun temperature(delta: Float) {
        state = state.copy(temperature = (state.temperature + delta).coerceIn(16f, 30f), powerOn = true)
    }
    fun setTemperature(value: Float) {
        state = state.copy(temperature = value.coerceIn(16f, 30f), powerOn = true)
    }
    fun setFan(value: Int) {
        state = state.copy(fanLevel = value.coerceIn(1, 7), powerOn = true)
    }
    fun togglePower() { state = state.copy(powerOn = !state.powerOn) }
    fun toggleAuto() { state = state.copy(autoOn = !state.autoOn, powerOn = true) }
    fun toggleAc() { state = state.copy(acOn = !state.acOn, powerOn = true) }
    fun toggleRecirculation() { state = state.copy(recirculationOn = !state.recirculationOn, powerOn = true) }
    fun toggleFrontDefrost() { state = state.copy(frontDefrostOn = !state.frontDefrostOn, powerOn = true) }
    fun toggleRearDefrost() { state = state.copy(rearDefrostOn = !state.rearDefrostOn, powerOn = true) }

    fun processCommand(raw: String): String {
        val text = raw.replace(" ", "").replace("。", "").replace("℃", "度").lowercase()
        val replies = mutableListOf<String>()
        state = state.copy(lastCommand = raw)

        if (has(text, "エアコンオフ", "エアコンを切", "空調オフ", "空調を切")) {
            state = state.copy(powerOn = false); replies += "エアコンをオフにしました"
        } else if (has(text, "エアコンオン", "エアコンをつけ", "空調オン", "空調をつけ")) {
            state = state.copy(powerOn = true, acOn = true); replies += "エアコンをオンにしました"
        }

        if (has(text, "暑い", "温度を下げ", "涼しく", "冷やして")) {
            temperature(-2f); replies += "設定温度を${format(state.temperature)}度に下げました"
        }
        if (has(text, "寒い", "温度を上げ", "暖かく", "暖めて")) {
            temperature(2f); replies += "設定温度を${format(state.temperature)}度に上げました"
        }
        temperatureFrom(text)?.let {
            setTemperature(it); replies += "設定温度を${format(state.temperature)}度にしました"
        }
        if (has(text, "風量を上げ", "風を強く", "風量強く")) {
            setFan(state.fanLevel + 1); replies += "風量を${state.fanLevel}に上げました"
        }
        if (has(text, "風量を下げ", "風を弱く", "風量弱く")) {
            setFan(state.fanLevel - 1); replies += "風量を${state.fanLevel}に下げました"
        }
        if (has(text, "風量最大", "風を最大", "最大風量")) {
            setFan(7); replies += "風量を最大にしました"
        }
        if (has(text, "風量最小", "風を最小", "最小風量")) {
            setFan(1); replies += "風量を最小にしました"
        }
        if (has(text, "オートにして", "自動にして", "オートオン")) {
            state = state.copy(autoOn = true, powerOn = true); replies += "オート空調にしました"
        }
        if (has(text, "内気循環", "内気にして")) {
            state = state.copy(recirculationOn = true); replies += "内気循環にしました"
        }
        if (has(text, "外気導入", "外気にして")) {
            state = state.copy(recirculationOn = false); replies += "外気導入にしました"
        }
        if (has(text, "フロントデフロスター", "フロント曇り止め", "前の曇りを取")) {
            state = state.copy(frontDefrostOn = true, powerOn = true); replies += "フロントデフロスターをオンにしました"
        }
        if (has(text, "リアデフロスター", "リア曇り止め", "後ろの曇りを取")) {
            state = state.copy(rearDefrostOn = true); replies += "リアデフロスターをオンにしました"
        }

        val answer = if (replies.isEmpty())
            "すみません。エアコン操作として理解できませんでした。例えば、24度にして、と話してください"
        else replies.distinct().joinToString("。")
        state = state.copy(response = answer)
        return answer
    }

    private fun has(text: String, vararg keys: String) = keys.any(text::contains)
    private fun temperatureFrom(text: String): Float? {
        val match = Regex("(1[6-9]|2[0-9]|30)(?:\\.5)?(?:度|にして)").find(text) ?: return null
        return match.value.replace("度", "").replace("にして", "").toFloatOrNull()
    }
    private fun format(v: Float) = if (v == v.roundToInt().toFloat()) v.roundToInt().toString() else "%.1f".format(v)
}
