package com.example.voiceselforder

import android.Manifest
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

data class MenuItem(val name: String, val price: Int, val aliases: List<String>)
data class CartLine(val item: MenuItem, var qty: Int)

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var guideText: TextView
    private lateinit var heardText: TextView
    private lateinit var statusText: TextView
    private lateinit var cartText: TextView
    private lateinit var totalText: TextView
    private lateinit var micButton: Button
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private val cart = mutableListOf<CartLine>()
    private var lastAdded: CartLine? = null
    private var ttsReady = false

    private val menu = listOf(
        MenuItem("まぐろ", 180, listOf("まぐろ", "マグロ", "鮪")),
        MenuItem("サーモン", 220, listOf("サーモン")),
        MenuItem("えび", 200, listOf("えび", "エビ", "海老")),
        MenuItem("たまご", 150, listOf("たまご", "玉子", "卵")),
        MenuItem("いくら", 320, listOf("いくら", "イクラ")),
        MenuItem("生ビール", 550, listOf("生ビール", "ビール")),
        MenuItem("お茶", 0, listOf("お茶", "ちゃ"))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        guideText = findViewById(R.id.guideText)
        heardText = findViewById(R.id.heardText)
        statusText = findViewById(R.id.statusText)
        cartText = findViewById(R.id.cartText)
        totalText = findViewById(R.id.totalText)
        micButton = findViewById(R.id.micButton)
        tts = TextToSpeech(this, this)

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            micButton.isEnabled = false
            statusText.text = "音声認識を利用できません"
        } else {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer.setRecognitionListener(listener)
        }
        micButton.setOnClickListener { ensurePermissionAndListen() }
        findViewById<Button>(R.id.undoButton).setOnClickListener { undoLast() }
        findViewById<Button>(R.id.confirmButton).setOnClickListener { confirmOrder() }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.JAPAN
            tts.setSpeechRate(0.92f)
            ttsReady = true
            speak("いらっしゃいませ。マイクボタンを押して、ご注文をお話しください。")
            statusText.text = "ご注文をどうぞ"
        }
    }

    private fun ensurePermissionAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        } else startListening()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == 100 && results.firstOrNull() == PackageManager.PERMISSION_GRANTED) startListening()
    }

    private fun startListening() {
        if (!::speechRecognizer.isInitialized) return
        tts.stop()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        speechRecognizer.startListening(intent)
        statusText.text = "聞いています…"
        guideText.text = "ご注文をお話しください"
        micButton.isEnabled = false
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { statusText.text = "聞いています…" }
        override fun onBeginningOfSpeech() { heardText.text = "…" }
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { statusText.text = "認識中…" }
        override fun onEvent(eventType: Int, params: Bundle?) {}
        override fun onPartialResults(bundle: Bundle?) {
            bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { heardText.text = it }
        }
        override fun onResults(bundle: Bundle?) {
            micButton.isEnabled = true
            val text = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            heardText.text = if (text.isBlank()) "聞き取れませんでした" else "「$text」"
            processUtterance(text)
        }
        override fun onError(error: Int) {
            micButton.isEnabled = true
            statusText.text = "もう一度お話しください"
            guideText.text = "聞き取れませんでした"
            if (error != SpeechRecognizer.ERROR_CLIENT) speak("すみません。もう一度、短くお話しください。")
        }
    }

    private fun processUtterance(raw: String) {
        val text = normalizeNumbers(raw)
        when {
            text.isBlank() -> return
            listOf("以上", "注文確定", "これで", "終わり").any { text.contains(it) } -> confirmOrder()
            listOf("取消", "取り消", "キャンセル", "戻して", "削除").any { text.contains(it) } -> undoLast()
            text.contains("全部") && listOf("取消", "削除", "キャンセル").any { text.contains(it) } -> clearCart()
            else -> addRecognizedItems(text)
        }
    }

    private fun addRecognizedItems(text: String) {
        val found = menu.mapNotNull { item ->
            if (item.aliases.any { text.contains(it) }) item to extractQuantity(text, item) else null
        }
        if (found.isEmpty()) {
            guideText.text = "商品名を確認できませんでした"
            statusText.text = "タッチまたは再発話"
            speak("商品名を確認できませんでした。例えば、サーモンを二皿、のようにお話しください。")
            return
        }
        val spoken = mutableListOf<String>()
        found.forEach { (item, qty) ->
            val existing = cart.firstOrNull { it.item.name == item.name }
            if (existing != null) existing.qty += qty else cart.add(CartLine(item, qty))
            lastAdded = CartLine(item, qty)
            spoken += "${item.name}を${qty}つ"
        }
        updateCart()
        val phrase = spoken.joinToString("、")
        guideText.text = "$phrase 追加しました"
        statusText.text = "続けて注文できます"
        speak("$phrase、追加しました。ほかにご注文はありますか？")
    }

    private fun extractQuantity(text: String, item: MenuItem): Int {
        val index = item.aliases.map { text.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: 0
        val around = text.substring(index).take(16)
        return Regex("([1-9]|10)").find(around)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(1, 10) ?: 1
    }

    private fun normalizeNumbers(s: String): String {
        val replacements = linkedMapOf("十" to "10", "一" to "1", "ひとつ" to "1", "二" to "2", "ふたつ" to "2", "三" to "3", "みっつ" to "3", "四" to "4", "よっつ" to "4", "五" to "5", "六" to "6", "七" to "7", "八" to "8", "九" to "9")
        var out = s
        replacements.forEach { (a,b) -> out = out.replace(a,b) }
        return out
    }

    private fun undoLast() {
        val last = lastAdded ?: cart.lastOrNull()?.let { CartLine(it.item, 1) }
        if (last == null) { speak("取り消す商品はありません。"); return }
        val target = cart.firstOrNull { it.item.name == last.item.name } ?: return
        val removeQty = minOf(last.qty, target.qty)
        target.qty -= removeQty
        if (target.qty <= 0) cart.remove(target)
        lastAdded = null
        updateCart()
        guideText.text = "${last.item.name}を取り消しました"
        speak("${last.item.name}を取り消しました。")
    }

    private fun clearCart() {
        cart.clear(); lastAdded = null; updateCart()
        guideText.text = "注文をすべて取り消しました"
        speak("注文をすべて取り消しました。")
    }

    private fun confirmOrder() {
        if (cart.isEmpty()) { speak("ご注文がまだありません。"); return }
        val summary = cart.joinToString("、") { "${it.item.name}${it.qty}つ" }
        guideText.text = "注文を確定しました"
        statusText.text = "ありがとうございました"
        speak("$summary、合計${cart.sumOf { it.item.price * it.qty }}円で承りました。ありがとうございます。")
        Toast.makeText(this, "注文を確定しました", Toast.LENGTH_LONG).show()
    }

    private fun updateCart() {
        cartText.text = if (cart.isEmpty()) "まだ商品はありません" else cart.joinToString("\n") { "${it.item.name}  × ${it.qty}　　¥${it.item.price * it.qty}" }
        totalText.text = "合計　¥${cart.sumOf { it.item.price * it.qty }}"
    }

    private fun speak(text: String) {
        if (ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "guide")
    }

    override fun onDestroy() {
        if (::speechRecognizer.isInitialized) speechRecognizer.destroy()
        tts.stop(); tts.shutdown()
        super.onDestroy()
    }
}
