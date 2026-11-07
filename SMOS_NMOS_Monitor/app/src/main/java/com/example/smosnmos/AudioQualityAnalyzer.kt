package com.example.smosnmos

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlin.math.*

data class QualityMetrics(
    val smos: Float = 1f,
    val nmos: Float = 1f,
    val rmsDb: Float = -90f,
    val snrDb: Float = 0f,
    val clipping: Float = 0f,
    val speechActive: Boolean = false,
    val waveform: List<Float> = List(96) { 0f }
)

class AudioQualityAnalyzer(private val activity: MainActivity) {
    @Volatile private var running = false
    private val sampleRate = 16000
    private var noiseFloor = -55.0

    @SuppressLint("MissingPermission")
    suspend fun run(onMetrics: (QualityMetrics) -> Unit) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        val min = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) return
        val record = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(min * 2, 4096))
        val pcm = ShortArray(2048)
        running = true
        try {
            record.startRecording()
            while (running) {
                val n = record.read(pcm, 0, pcm.size)
                if (n > 0) onMetrics(analyze(pcm, n)) else delay(20)
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }

    fun stop() { running = false }

    private fun analyze(x: ShortArray, n: Int): QualityMetrics {
        var power = 0.0
        var clipped = 0
        var zeroCross = 0
        for (i in 0 until n) {
            val v = x[i] / 32768.0
            power += v * v
            if (abs(x[i].toInt()) > 32100) clipped++
            if (i > 0 && (x[i] >= 0) != (x[i - 1] >= 0)) zeroCross++
        }
        val rms = sqrt(power / n.coerceAtLeast(1))
        val db = 20.0 * log10(rms.coerceAtLeast(1e-7))
        val speech = db > noiseFloor + 7.0 && db > -45.0
        if (!speech) noiseFloor = noiseFloor * 0.985 + db.coerceIn(-85.0, -25.0) * 0.015
        val snr = (db - noiseFloor).coerceIn(0.0, 40.0)
        val clipRate = clipped.toDouble() / n
        val zcr = zeroCross.toDouble() / n

        // Explainable, device-local proxy scores. These are not ITU/P.835 or DNSMOS results.
        val noisePenalty = ((noiseFloor + 65.0) / 10.0).coerceIn(0.0, 3.0)
        val nmos = (4.8 - noisePenalty).coerceIn(1.0, 5.0)
        val levelPenalty = when {
            db < -45 -> (-45 - db) / 12.0
            db > -8 -> (db + 8) / 5.0
            else -> 0.0
        }
        val clipPenalty = (clipRate * 80.0).coerceAtMost(2.5)
        val zcrPenalty = ((zcr - 0.22) * 5.0).coerceAtLeast(0.0)
        val smos = if (speech) (4.7 - levelPenalty - clipPenalty - zcrPenalty).coerceIn(1.0, 5.0) else 1.0
        val wave = (0 until 96).map { i -> x[(i * n / 96).coerceAtMost(n - 1)] / 32768f }
        return QualityMetrics(smos.toFloat(), nmos.toFloat(), db.toFloat(), snr.toFloat(),
            (clipRate * 100).toFloat(), speech, wave)
    }
}
