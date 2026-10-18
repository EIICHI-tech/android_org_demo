package jp.co.kagafei.earmg510

import android.app.Application
import android.hardware.usb.UsbDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EArmViewModel(application: Application) : AndroidViewModel(application) {
    private val client = EArmUsbClient(application)
    
    private val _on = MutableStateFlow(false)
    val on: StateFlow<Boolean> = _on.asStateFlow()
    
    private val _msg = MutableStateFlow("USB-CでeArmを接続してください")
    val msg: StateFlow<String> = _msg.asStateFlow()
    
    private val _act = MutableStateFlow<String?>(null)
    val act: StateFlow<String?> = _act.asStateFlow()

    fun discover() {
        val device = client.find()
        when {
            device == null -> {
                _on.value = false
                _msg.value = "CH340未検出: OTGケーブルを確認"
            }
            !client.permitted(device) -> {
                _msg.value = "USB使用許可を要求中"
                client.request(device)
            }
            else -> {
                connect(device)
            }
        }
    }

    fun connect(device: UsbDevice) {
        viewModelScope.launch {
            val result = client.open(device)
            if (result.isSuccess) {
                _on.value = true
                _msg.value = "USB接続済み 115200 bps"
            } else {
                val exception = result.exceptionOrNull()
                _on.value = false
                _msg.value = "接続エラー: ${exception?.message}"
            }
        }
    }

    fun press(action: String) {
        if (!_on.value) return
        _act.value = action
        viewModelScope.launch {
            val result = client.send("$action 1")
            if (result.isFailure) {
                val exception = result.exceptionOrNull()
                handleFailure(exception ?: IllegalStateException("Unknown error"))
            }
        }
    }

    fun release(action: String) {
        if (_act.value == action) {
            _act.value = null
        }
        viewModelScope.launch {
            val result = client.send("$action 0")
            if (result.isFailure) {
                val exception = result.exceptionOrNull()
                handleFailure(exception ?: IllegalStateException("Unknown error"))
            }
        }
    }

    fun stop(reason: String = "緊急停止しました") {
        _act.value = null
        viewModelScope.launch {
            client.send("STOP")
            _msg.value = reason
        }
    }

    fun buzzer(enabled: Boolean) {
        viewModelScope.launch {
            client.send("BUZZER ${if (enabled) 1 else 0}")
        }
    }

    fun disconnect() {
        stop("USB切断")
        client.close()
        _on.value = false
    }

    private fun handleFailure(throwable: Throwable) {
        _on.value = false
        _msg.value = "送信エラー: ${throwable.message}"
    }

    override fun onCleared() {
        client.close()
        super.onCleared()
    }
}
