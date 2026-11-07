package com.example.vocalswitchkaraoke

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.loadOrCueVideo
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContent { KaraokeV2App() }
    }
}

enum class PlayMode(val label: String) {
    ORIGINAL("原曲"), KARAOKE("歌手OFF"), VOCAL_ONLY("歌手のみ"), MIX("ミックス")
}

class KaraokeViewModel(app: Application) : AndroidViewModel(app) {
    private val context = app.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _status = MutableStateFlow("YouTube URLを読み込み、伴奏と歌声を選択してください")
    val status: StateFlow<String> = _status

    var instrumentalUri by mutableStateOf<Uri?>(null); private set
    var vocalUri by mutableStateOf<Uri?>(null); private set
    var mode by mutableStateOf(PlayMode.ORIGINAL);
    private set
    var musicVolume by mutableFloatStateOf(0.85f)
    var vocalVolume by mutableFloatStateOf(0.70f)
    var syncOffsetMs by mutableIntStateOf(0)
    var youtubeSecond by mutableFloatStateOf(0f); private set
    var youtubePlaying by mutableStateOf(false); private set
    var playerReady by mutableStateOf(false); private set

    private var youtubePlayer: YouTubePlayer? = null
    private var videoId: String? = null
    private val musicPlayer = ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_OFF }
    private val singerPlayer = ExoPlayer.Builder(context).build().apply { repeatMode = Player.REPEAT_MODE_OFF }
    private val syncRunnable = object : Runnable {
        override fun run() {
            if (youtubePlaying && mode != PlayMode.ORIGINAL) correctSync()
            mainHandler.postDelayed(this, 500)
        }
    }
    val recorder = WavRecorder(context) { text -> _status.value = text }

    init {
        val attrs = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build()
        musicPlayer.setAudioAttributes(attrs, true)
        singerPlayer.setAudioAttributes(attrs, false)
        mainHandler.post(syncRunnable)
    }

    fun bindYouTubePlayer(player: YouTubePlayer) {
        youtubePlayer = player
        playerReady = true
        _status.value = "YouTubeプレーヤー準備完了"
        videoId?.let { player.cueVideo(it, 0f) }
    }

    fun loadYouTube(input: String) {
        val id = extractVideoId(input)
        if (id == null) { _status.value = "URLまたは11文字の動画IDを確認してください"; return }
        videoId = id
        youtubePlayer?.cueVideo(id, 0f)
        _status.value = if (playerReady) "動画を準備しました" else "プレーヤー準備後に動画を読み込みます"
    }

    fun onYoutubeTime(second: Float) { youtubeSecond = second }
    fun onYoutubeState(playing: Boolean) {
        youtubePlaying = playing
        if (playing && mode != PlayMode.ORIGINAL) { seekLocal(); startLocal() } else pauseLocal()
    }

    fun setInstrumental(uri: Uri) { instrumentalUri = uri; persist(uri); prepare(musicPlayer, uri, "伴奏") }
    fun setVocal(uri: Uri) { vocalUri = uri; persist(uri); prepare(singerPlayer, uri, "歌声") }
    private fun persist(uri: Uri) { runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
    private fun prepare(player: ExoPlayer, uri: Uri, name: String) {
        player.setMediaItem(MediaItem.fromUri(uri)); player.prepare(); _status.value = "$name WAVを準備中"
    }

    fun updateMode(newMode: PlayMode) {
        mode = newMode
        if (newMode == PlayMode.ORIGINAL) {
            pauseLocal(); youtubePlayer?.unMute()
        } else {
            youtubePlayer?.mute(); seekLocal(); if (youtubePlaying) startLocal()
        }
        applyVolumes()
    }

    fun applyVolumes() {
        musicPlayer.volume = if (mode == PlayMode.VOCAL_ONLY) 0f else musicVolume
        singerPlayer.volume = if (mode == PlayMode.KARAOKE) 0f else vocalVolume
    }

    fun togglePlayback() {
        if (youtubePlaying) youtubePlayer?.pause() else youtubePlayer?.play()
    }

    fun resync() { seekLocal(); _status.value = "同期位置を補正しました: ${syncOffsetMs} ms" }
    private fun targetMs() = ((youtubeSecond * 1000).toLong() + syncOffsetMs).coerceAtLeast(0)
    private fun seekLocal() { val t = targetMs(); musicPlayer.seekTo(t); singerPlayer.seekTo(t) }
    private fun correctSync() {
        val target = targetMs()
        if (abs(musicPlayer.currentPosition - target) > 180L || abs(singerPlayer.currentPosition - target) > 180L) seekLocal()
    }
    private fun startLocal() {
        if (instrumentalUri == null || vocalUri == null) { _status.value = "伴奏WAVと歌声WAVの両方を選択してください"; return }
        applyVolumes(); musicPlayer.play(); singerPlayer.play()
    }
    private fun pauseLocal() { musicPlayer.pause(); singerPlayer.pause() }

    private fun extractVideoId(text: String): String? {
        val s = text.trim()
        if (Regex("^[A-Za-z0-9_-]{11}$").matches(s)) return s
        val patterns = listOf("[?&]v=([A-Za-z0-9_-]{11})", "youtu\\.be/([A-Za-z0-9_-]{11})", "shorts/([A-Za-z0-9_-]{11})", "embed/([A-Za-z0-9_-]{11})")
        return patterns.firstNotNullOfOrNull { Regex(it).find(s)?.groupValues?.get(1) }
    }

    override fun onCleared() {
        mainHandler.removeCallbacks(syncRunnable); recorder.stop(); musicPlayer.release(); singerPlayer.release(); super.onCleared()
    }
}

@Composable
fun KaraokeV2App(vm: KaraokeViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val status by vm.status.collectAsState()
    var url by remember { mutableStateOf("") }
    var micGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val requestMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> micGranted = granted; if (granted) vm.recorder.start() }
    val requestBt = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val pickMusic = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::setInstrumental) }
    val pickVocal = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::setVocal) }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) requestBt.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF40C4FF), secondary = Color(0xFFB2FF59))) {
        Row(Modifier.fillMaxSize().background(Color(0xFF090D13)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.weight(1.55f).fillMaxHeight()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        YouTubePlayerView(ctx).apply {
                            lifecycleOwner.lifecycle.addObserver(this)
                            addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                                override fun onReady(youTubePlayer: YouTubePlayer) { vm.bindYouTubePlayer(youTubePlayer) }
                                override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) { vm.onYoutubeTime(second) }
                                override fun onStateChange(youTubePlayer: YouTubePlayer, state: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState) {
                                    vm.onYoutubeState(state == com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.PLAYING)
                                }
                            })
                        }
                    }
                )
            }
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Vocal Switch Karaoke V2", style = MaterialTheme.typography.headlineSmall)
                Text(if (vm.playerReady) "YouTube: READY" else "YouTube: 初期化中", color = if (vm.playerReady) Color(0xFFB2FF59) else Color.Yellow)
                OutlinedTextField(url, { url = it }, label = { Text("YouTube URL / Video ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button({ vm.loadYouTube(url) }, Modifier.fillMaxWidth()) { Text("YouTubeを読み込む") }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button({ pickMusic.launch(arrayOf("audio/wav", "audio/*")) }, Modifier.weight(1f)) { Text("伴奏WAV") }
                    Button({ pickVocal.launch(arrayOf("audio/wav", "audio/*")) }, Modifier.weight(1f)) { Text("歌声WAV") }
                }
                Text("伴奏: ${vm.instrumentalUri?.lastPathSegment ?: "未選択"}", maxLines = 1, color = Color.LightGray)
                Text("歌声: ${vm.vocalUri?.lastPathSegment ?: "未選択"}", maxLines = 1, color = Color.LightGray)
                PlayMode.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { row.forEach { item -> FilterChip(vm.mode == item, { vm.updateMode(item) }, { Text(item.label) }, modifier = Modifier.weight(1f)) } }
                }
                Text("伴奏音量 ${(vm.musicVolume * 100).toInt()}%")
                Slider(vm.musicVolume, { vm.musicVolume = it; vm.applyVolumes() })
                Text("歌手音量 ${(vm.vocalVolume * 100).toInt()}%")
                Slider(vm.vocalVolume, { vm.vocalVolume = it; vm.applyVolumes() })
                Text("同期補正 ${vm.syncOffsetMs} ms")
                Slider(vm.syncOffsetMs.toFloat(), { vm.syncOffsetMs = it.toInt(); vm.resync() }, valueRange = -500f..500f, steps = 19)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(vm::togglePlayback, Modifier.weight(1f)) { Text("再生 / 停止") }
                    OutlinedButton(vm::resync, Modifier.weight(1f)) { Text("再同期") }
                }
                Button({ if (vm.recorder.recording) vm.recorder.stop() else if (micGranted) vm.recorder.start() else requestMic.launch(Manifest.permission.RECORD_AUDIO) }, Modifier.fillMaxWidth()) {
                    Text(if (vm.recorder.recording) "録音停止" else "マイク録音")
                }
                Text(status, color = Color(0xFF80D8FF))
                Text("YouTube: %.1f 秒".format(vm.youtubeSecond), color = Color.Gray)
                Text("YouTube音声は抽出しません。権利処理済みの分離WAVを使用してください。", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFCC80))
            }
        }
    }
}

class WavRecorder(private val context: Context, private val report: (String) -> Unit) {
    private val active = AtomicBoolean(false)
    private val main = Handler(Looper.getMainLooper())
    var recording by mutableStateOf(false); private set
    private var audioRecord: AudioRecord? = null
    private var worker: Thread? = null

    fun start() {
        if (active.get()) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { report("マイク権限が必要です"); return }
        val rate = 48000
        val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) { report("AudioRecordを初期化できません"); return }
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
        val output = File(dir, "karaoke_${System.currentTimeMillis()}.wav")
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, min * 2)
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) { audioRecord?.release(); audioRecord = null; report("マイクを初期化できません"); return }
        active.set(true); recording = true; report("マイク録音中")
        worker = Thread {
            var count = 0L
            val buffer = ByteArray(min)
            try {
                RandomAccessFile(output, "rw").use { file ->
                    writeHeader(file, rate, 0); audioRecord?.startRecording()
                    while (active.get()) { val n = audioRecord?.read(buffer, 0, buffer.size) ?: 0; if (n > 0) { file.write(buffer, 0, n); count += n } }
                    runCatching { audioRecord?.stop() }; file.seek(0); writeHeader(file, rate, count)
                }
                main.post { recording = false; report("録音保存: ${output.absolutePath}") }
            } catch (e: Exception) { main.post { recording = false; report("録音エラー: ${e.message}") } }
            finally { audioRecord?.release(); audioRecord = null }
        }.also { it.start() }
    }
    fun stop() { if (!active.getAndSet(false)) return }
    private fun writeHeader(file: RandomAccessFile, rate: Int, data: Long) {
        fun text(v: String) = file.write(v.toByteArray(Charsets.US_ASCII))
        fun le32(v: Long) = file.write(byteArrayOf((v and 255).toByte(), ((v shr 8) and 255).toByte(), ((v shr 16) and 255).toByte(), ((v shr 24) and 255).toByte()))
        fun le16(v: Int) = file.write(byteArrayOf((v and 255).toByte(), ((v shr 8) and 255).toByte()))
        text("RIFF"); le32(36 + data); text("WAVEfmt "); le32(16); le16(1); le16(1); le32(rate.toLong()); le32((rate * 2).toLong()); le16(2); le16(16); text("data"); le32(data)
    }
}
