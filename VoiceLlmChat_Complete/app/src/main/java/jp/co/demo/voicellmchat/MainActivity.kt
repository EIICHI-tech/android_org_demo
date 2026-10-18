package jp.co.demo.voicellmchat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        textToSpeech = TextToSpeech(this, this)
        setContent { MaterialTheme { VoiceLlmApp() } }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.JAPAN
            textToSpeech?.setSpeechRate(1.08f)
        }
    }

    private fun speak(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "llm-answer")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun VoiceLlmApp(vm: MainViewModel = viewModel()) {
        val prefs = remember { getSharedPreferences("voice_llm_settings", MODE_PRIVATE) }
        var endpoint by remember { mutableStateOf(prefs.getString("endpoint", "http://192.168.0.10:11434") ?: "") }
        var apiKey by remember { mutableStateOf(prefs.getString("apiKey", "") ?: "") }
        var model by remember { mutableStateOf(prefs.getString("model", "qwen2.5:3b") ?: "") }
        var settingsOpen by remember { mutableStateOf(false) }
        var listening by remember { mutableStateOf(false) }
        val listState = rememberLazyListState()

        val microphonePermission = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) startListening(vm, endpoint, apiKey, model) { listening = it }
            else vm.status.value = "マイク権限を許可してください"
        }

        LaunchedEffect(Unit) { vm.onSpeak = ::speak }
        LaunchedEffect(vm.messages.size, vm.partial.value) {
            val count = vm.messages.size + if (vm.partial.value.isNotBlank()) 1 else 0
            if (count > 0) listState.animateScrollToItem(count - 1)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("音声LLM会話") },
                    actions = {
                        TextButton(onClick = { settingsOpen = true }) { Text("設定") }
                        TextButton(onClick = vm::clear) { Text("消去") }
                    }
                )
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().padding(12.dp)) {
                Text(vm.status.value, color = if (vm.busy.value) Color(0xFF4F46E5) else Color.DarkGray)
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(vm.messages) { message ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (message.role == "user") Color(0xFFE0E7FF) else Color(0xFFF1F5F9)
                            )
                        ) {
                            Text(
                                (if (message.role == "user") "あなた: " else "AI: ") + message.text,
                                Modifier.padding(12.dp)
                            )
                        }
                    }
                    if (vm.partial.value.isNotBlank()) {
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Text("AI: ${vm.partial.value}▌", Modifier.padding(12.dp))
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = vm.draft.value,
                        onValueChange = { vm.draft.value = it },
                        label = { Text("話すか文字を入力") },
                        modifier = Modifier.weight(1f),
                        maxLines = 3
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { vm.send(endpoint, apiKey, model) },
                        enabled = !vm.busy.value
                    ) { Text("送信") }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            startListening(vm, endpoint, apiKey, model) { listening = it }
                        } else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    enabled = !vm.busy.value
                ) { Text(if (listening) "聞いています…" else "マイクを押して話す") }
                if (vm.busy.value) {
                    TextButton(onClick = vm::stop, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("生成を停止")
                    }
                }
            }
        }

        if (settingsOpen) {
            AlertDialog(
                onDismissRequest = { settingsOpen = false },
                title = { Text("LLM接続設定") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(endpoint, { endpoint = it }, label = { Text("Endpoint") })
                        OutlinedTextField(model, { model = it }, label = { Text("Model") })
                        OutlinedTextField(apiKey, { apiKey = it }, label = { Text("API Key（必要な場合）") })
                        Text("例: Endpoint = http://192.168.0.10:11434、Model = qwen2.5:3b", style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        prefs.edit().putString("endpoint", endpoint).putString("model", model).putString("apiKey", apiKey).apply()
                        settingsOpen = false
                    }) { Text("保存") }
                },
                dismissButton = { TextButton(onClick = { settingsOpen = false }) { Text("閉じる") } }
            )
        }
    }

    private fun startListening(
        vm: MainViewModel,
        endpoint: String,
        apiKey: String,
        model: String,
        updateListening: (Boolean) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            vm.status.value = "この端末で利用可能な音声認識サービスが見つかりません"
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                updateListening(true)
                vm.status.value = "どうぞお話しください"
            }
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                    vm.draft.value = it
                }
            }
            override fun onResults(results: Bundle?) {
                updateListening(false)
                val recognized = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                vm.draft.value = recognized
                if (recognized.isNotBlank()) vm.send(endpoint, apiKey, model, recognized)
            }
            override fun onError(error: Int) {
                updateListening(false)
                vm.status.value = "音声認識エラー: $error。もう一度マイクを押してください"
            }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { updateListening(false) }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }
        speechRecognizer?.startListening(intent)
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }
}
