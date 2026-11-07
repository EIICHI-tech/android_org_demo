package com.example.emergencyalertdemo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.emergencyalertdemo.databinding.ActivityMainBinding
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var b: ActivityMainBinding
    private var tts: TextToSpeech? = null
    private val running = AtomicBoolean(false)
    private var recorder: AudioRecord? = null
    private var lastAlertAt = 0L
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) startMonitoring() else showIdle("マイク権限が必要です") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); b = ActivityMainBinding.inflate(layoutInflater); setContentView(b.root)
        tts = TextToSpeech(this, this)
        b.startButton.setOnClickListener { if (running.get()) stopMonitoring() else ensurePermission() }
        b.testButton.setOnClickListener { alert("警報テスト", "周囲の安全を確認してください") }
    }

    private fun ensurePermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startMonitoring()
        else permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startMonitoring() {
        if (running.getAndSet(true)) return
        b.startButton.text = "監視停止"; b.statusText.text = "監視中"; b.statusText.setBackgroundColor(Color.rgb(8,127,91))
        b.detailText.text = "救急車・パトカー・警報音の特徴を解析中"
        thread(name="EmergencyAudioMonitor") { monitorLoop() }
    }

    private fun monitorLoop() {
        val sr = 16000; val n = 2048
        val min = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val r = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(min, n*4))
        recorder = r
        val buf = ShortArray(n); val history = ArrayDeque<Double>()
        try {
            r.startRecording()
            while (running.get()) {
                val got = r.read(buf,0,n)
                if (got <= 0) continue
                val rms = sqrt(buf.take(got).sumOf { it.toDouble()*it } / got) / 32768.0
                val db = 20.0*log10(rms.coerceAtLeast(1e-7))
                val peak = dominantFrequency(buf, got, sr)
                history.addLast(peak); while(history.size>12) history.removeFirst()
                val swing = if(history.size>5) (history.maxOrNull()!!-history.minOrNull()!!) else 0.0
                val sirenLike = db > -32 && peak in 450.0..1800.0 && swing > 180.0
                runOnUiThread { b.levelBar.progress = ((db+60)*1.7).roundToInt().coerceIn(0,100); b.detailText.text="音量 %.1f dBFS / 主成分 %.0f Hz / 変動 %.0f Hz".format(db,peak,swing) }
                if(sirenLike && System.currentTimeMillis()-lastAlertAt>7000) { lastAlertAt=System.currentTimeMillis(); runOnUiThread { alert("緊急車両・警報音の可能性", "周囲を確認し、安全な場所へ移動してください") } }
            }
        } catch(e: Exception) { runOnUiThread { showIdle("録音エラー: ${e.message}") } }
        finally { try{r.stop()}catch(_:Exception){}; r.release(); recorder=null }
    }

    // Lightweight demo spectrum scan. Replace with a TFLite/YAMNet-style classifier for production.
    private fun dominantFrequency(x: ShortArray, count:Int, sr:Int):Double {
        var bestF=0.0; var best=0.0
        for(f in 300..2200 step 25) {
            var re=0.0; var im=0.0
            for(i in 0 until count) { val w=0.5-0.5*cos(2.0*Math.PI*i/(count-1)); val a=2.0*Math.PI*f*i/sr; re+=x[i]*w*cos(a); im-=x[i]*w*sin(a) }
            val p=re*re+im*im; if(p>best){best=p;bestF=f.toDouble()}
        }
        return bestF
    }

    private fun alert(title:String, message:String) {
        b.statusText.text=title; b.statusText.setBackgroundColor(Color.rgb(176,0,32)); b.detailText.text=message
        tts?.speak("警告。$title。$message", TextToSpeech.QUEUE_FLUSH, null, "alert")
    }
    private fun stopMonitoring(){ running.set(false); recorder?.stop(); showIdle("監視を停止しました") }
    private fun showIdle(msg:String){ b.startButton.text="監視開始"; b.statusText.text="停止中"; b.statusText.setBackgroundColor(Color.rgb(38,50,56)); b.detailText.text=msg }
    override fun onInit(status:Int){ if(status==TextToSpeech.SUCCESS){ tts?.language=Locale.JAPAN; tts?.setSpeechRate(0.92f) } }
    override fun onDestroy(){ running.set(false); recorder?.release(); tts?.stop(); tts?.shutdown(); super.onDestroy() }
}
