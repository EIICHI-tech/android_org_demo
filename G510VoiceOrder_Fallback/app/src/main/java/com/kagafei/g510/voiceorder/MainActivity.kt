package com.kagafei.g510.voiceorder

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var input: EditText
    private lateinit var orderResult: TextView
    private val orders = mutableListOf<String>()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchRecognizer() else showManual("マイク権限がありません")
    }

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val candidates = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = candidates?.firstOrNull().orEmpty()
            if (text.isNotBlank()) {
                input.setText(text)
                status.text = "認識結果を確認して「注文に追加」を押してください"
            } else showManual("認識結果が空でした")
        } else {
            showManual("音声入力を完了できませんでした")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        input = findViewById(R.id.input)
        orderResult = findViewById(R.id.orderResult)

        findViewById<Button>(R.id.voiceButton).setOnClickListener { startVoice() }
        findViewById<Button>(R.id.addButton).setOnClickListener { addOrder() }
        findViewById<Button>(R.id.clearButton).setOnClickListener {
            orders.clear(); input.text.clear(); renderOrders(); status.text = "注文をクリアしました"
        }
    }

    private fun startVoice() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            launchRecognizer()
        } else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun launchRecognizer() {
        // G510対策: SpeechRecognizer.isRecognitionAvailable() は使用しない。
        // 実際にIntentを起動し、失敗時だけ安全に手入力へフォールバックする。
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "ご注文をお話しください")
        }
        try {
            speechLauncher.launch(intent)
            status.text = "音声を聞いています"
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No activity handles RECOGNIZE_SPEECH", e)
            showManual("端末共通の音声認識画面がないため、手入力モードに切り替えました")
        } catch (e: Exception) {
            Log.e(TAG, "Speech launch failed", e)
            showManual("音声機能を開始できないため、手入力モードに切り替えました")
        }
    }

    private fun showManual(message: String) {
        status.text = message
        input.requestFocus()
        input.post { input.selectAll() }
    }

    private fun addOrder() {
        val value = input.text.toString().trim()
        if (value.isEmpty()) {
            status.text = "商品名または注文内容を入力してください"
            return
        }
        orders += value
        input.text.clear()
        renderOrders()
        status.text = "注文に追加しました"
    }

    private fun renderOrders() {
        orderResult.text = if (orders.isEmpty()) "注文内容はここに表示されます"
        else orders.mapIndexed { index, item -> "${index + 1}. $item" }.joinToString("\n")
    }

    companion object { private const val TAG = "G510VoiceOrder" }
}
