package jp.co.demo.g510fft

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "FFT_AUDIO"
        private const val REQUEST_RECORD_AUDIO = 10

        private const val SAMPLE_RATE = 48000
        private const val FFT_SIZE = 1024
    }

    private lateinit var spectrum: SpectrumView
    private lateinit var status: TextView
    private lateinit var nrSwitch: Switch

    private val running = AtomicBoolean(false)

    private var worker: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        spectrum = SpectrumView(this)

        status = TextView(this).apply {
            textSize = 18f
            setPadding(16, 8, 16, 8)
            text = "Stopped"
        }

        nrSwitch = Switch(this).apply {
            text = "Noise Reduction"
            isChecked = true

            setOnCheckedChangeListener { _, enabled ->
                spectrum.nrEnabled = enabled
            }
        }

        val startButton = Button(this).apply {
            text = "START"

            setOnClickListener {
                ensurePermissionAndStart()
            }
        }

        val stopButton = Button(this).apply {
            text = "STOP"

            setOnClickListener {
                stopAudio()
            }
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL

            addView(startButton)
            addView(stopButton)
            addView(nrSwitch)

            addView(
                status,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )
        }

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL

                addView(bar)

                addView(
                    spectrum,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
                )
            }
        )

        /*
         * アプリ起動時に
         * Android Frameworkから見えている
         * Input Deviceを表示
         */
        dumpInputDevices()
    }

    /*
     * Androidから現在見えているInput DeviceをLogcatへ表示
     */
    private fun dumpInputDevices() {

        val audioManager =
            getSystemService(AudioManager::class.java)

        val devices =
            audioManager.getDevices(
                AudioManager.GET_DEVICES_INPUTS
            )

        Log.d(TAG, "========================================")
        Log.d(TAG, "Android Audio Input Device List")
        Log.d(TAG, "Input device count = ${devices.size}")
        Log.d(TAG, "========================================")

        for (device in devices) {

            Log.d(
                TAG,
                "INPUT: " +
                        "id=${device.id} " +
                        "type=${device.type} " +
                        "typeName=${audioDeviceTypeName(device.type)} " +
                        "name=${device.productName} " +
                        "address=${device.address}"
            )
        }

        Log.d(TAG, "========================================")
    }

    /*
     * AudioDeviceInfo.typeを読みやすい文字列へ変換
     *
     * 特に重要:
     *
     * TYPE_BUS = 21
     * TYPE_REMOTE_SUBMIX = 25
     *
     * type=25はI2Sではない。
     */
    private fun audioDeviceTypeName(type: Int): String {

        return when (type) {

            AudioDeviceInfo.TYPE_UNKNOWN ->
                "UNKNOWN"

            AudioDeviceInfo.TYPE_BUILTIN_MIC ->
                "BUILTIN_MIC"

            AudioDeviceInfo.TYPE_LINE_ANALOG ->
                "LINE_ANALOG"

            AudioDeviceInfo.TYPE_LINE_DIGITAL ->
                "LINE_DIGITAL"

            AudioDeviceInfo.TYPE_AUX_LINE ->
                "AUX_LINE"

            AudioDeviceInfo.TYPE_BUS ->
                "BUS"

            AudioDeviceInfo.TYPE_REMOTE_SUBMIX ->
                "REMOTE_SUBMIX"

            AudioDeviceInfo.TYPE_USB_DEVICE ->
                "USB_DEVICE"

            AudioDeviceInfo.TYPE_USB_HEADSET ->
                "USB_HEADSET"

            AudioDeviceInfo.TYPE_WIRED_HEADSET ->
                "WIRED_HEADSET"

            AudioDeviceInfo.TYPE_BLUETOOTH_SCO ->
                "BLUETOOTH_SCO"

            else ->
                "OTHER_$type"
        }
    }

    /*
     * Record Audio Permission
     */
    private fun ensurePermissionAndStart() {

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO
            )

        } else {

            startAudio()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode == REQUEST_RECORD_AUDIO &&
            grantResults.firstOrNull() ==
            PackageManager.PERMISSION_GRANTED
        ) {

            startAudio()
        }
    }

    /*
     * I2S候補デバイスを探す
     *
     * 優先順位
     *
     * LINE_ANALOG
     * LINE_DIGITAL
     * AUX_LINE
     * BUS
     *
     * REMOTE_SUBMIXは対象外
     */
    private fun findI2sInputDevice(
        devices: Array<AudioDeviceInfo>
    ): AudioDeviceInfo? {

        val lineAnalog =
            devices.firstOrNull {
                it.type ==
                        AudioDeviceInfo.TYPE_LINE_ANALOG
            }

        if (lineAnalog != null) {

            Log.d(
                TAG,
                "I2S candidate found as LINE_ANALOG"
            )

            return lineAnalog
        }

        val lineDigital =
            devices.firstOrNull {
                it.type ==
                        AudioDeviceInfo.TYPE_LINE_DIGITAL
            }

        if (lineDigital != null) {

            Log.d(
                TAG,
                "I2S candidate found as LINE_DIGITAL"
            )

            return lineDigital
        }

        val auxLine =
            devices.firstOrNull {
                it.type ==
                        AudioDeviceInfo.TYPE_AUX_LINE
            }

        if (auxLine != null) {

            Log.d(
                TAG,
                "I2S candidate found as AUX_LINE"
            )

            return auxLine
        }

        val bus =
            devices.firstOrNull {
                it.type ==
                        AudioDeviceInfo.TYPE_BUS
            }

        if (bus != null) {

            Log.d(
                TAG,
                "I2S candidate found as BUS"
            )

            return bus
        }

        return null
    }

    /*
     * Audio Capture開始
     *
     * 目標:
     *
     * IntelliGo
     *      |
     *      | I2S
     *      v
     * MT8370 I2SIN
     *      |
     *      v
     * AFE UL2
     *      |
     *      v
     * CAPTURE_UL2_FROM_I2SIN
     *      |
     *      v
     * MediaTek Audio HAL
     *      |
     *      v
     * AudioRecord
     *      |
     *      v
     * FFT
     */
    private fun startAudio() {

        if (running.getAndSet(true)) {
            return
        }

        worker = Thread {

            Process.setThreadPriority(
                Process.THREAD_PRIORITY_AUDIO
            )

            Log.d(
                TAG,
                "========================================"
            )

            Log.d(
                TAG,
                "Starting Audio Capture"
            )

            Log.d(
                TAG,
                "SampleRate=$SAMPLE_RATE"
            )

            Log.d(
                TAG,
                "FFT_SIZE=$FFT_SIZE"
            )

            /*
             * Minimum Audio Buffer
             */
            val minBuffer =
                AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

            Log.d(
                TAG,
                "AudioRecord minBuffer=$minBuffer"
            )

            if (minBuffer <= 0) {

                Log.e(
                    TAG,
                    "48kHz AudioRecord is not available"
                )

                runOnUiThread {
                    status.text =
                        "48 kHz input is not available"
                }

                running.set(false)
                return@Thread
            }

            /*
             * Permission Check
             */
            if (
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                Log.e(
                    TAG,
                    "RECORD_AUDIO permission missing"
                )

                running.set(false)
                return@Thread
            }

            /*
             * AudioRecord作成
             *
             * UNPROCESSEDは
             * I2Sを意味するものではない。
             *
             * Android Audio Policyが選択した
             * Physical Inputから
             * ProcessingなしのAudioを要求する。
             */
            val rec =
                try {

                    AudioRecord.Builder()

                        .setAudioSource(
                            MediaRecorder.AudioSource.UNPROCESSED
                        )

                        .setAudioFormat(
                            AudioFormat.Builder()

                                .setEncoding(
                                    AudioFormat.ENCODING_PCM_16BIT
                                )

                                .setSampleRate(
                                    SAMPLE_RATE
                                )

                                .setChannelMask(
                                    AudioFormat.CHANNEL_IN_MONO
                                )

                                .build()
                        )

                        .setBufferSizeInBytes(
                            max(
                                minBuffer,
                                FFT_SIZE * 8
                            )
                        )

                        .build()

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "AudioRecord creation failed",
                        e
                    )

                    runOnUiThread {
                        status.text =
                            "AudioRecord creation failed"
                    }

                    running.set(false)
                    return@Thread
                }

            /*
             * AudioRecord初期化確認
             */
            if (
                rec.state !=
                AudioRecord.STATE_INITIALIZED
            ) {

                Log.e(
                    TAG,
                    "AudioRecord is NOT initialized"
                )

                rec.release()

                runOnUiThread {
                    status.text =
                        "AudioRecord initialization failed"
                }

                running.set(false)

                return@Thread
            }

            Log.d(
                TAG,
                "AudioRecord initialized successfully"
            )

            /*
             * Android Frameworkから見える
             * Input Deviceを取得
             */
            val audioManager =
                getSystemService(AudioManager::class.java)

            val inputDevices =
                audioManager.getDevices(
                    AudioManager.GET_DEVICES_INPUTS
                )

            Log.d(
                TAG,
                "========================================"
            )

            Log.d(
                TAG,
                "Input devices before recording:"
            )

            for (device in inputDevices) {

                Log.d(
                    TAG,
                    "INPUT: " +
                            "id=${device.id} " +
                            "type=${device.type} " +
                            "typeName=${audioDeviceTypeName(device.type)} " +
                            "name=${device.productName} " +
                            "address=${device.address}"
                )
            }

            /*
             * REMOTE_SUBMIXを明示表示
             *
             * type=25は物理I2Sではない
             */
            val remoteSubmixDevices =
                inputDevices.filter {

                    it.type ==
                            AudioDeviceInfo.TYPE_REMOTE_SUBMIX
                }

            for (device in remoteSubmixDevices) {

                Log.w(
                    TAG,
                    "REMOTE_SUBMIX detected but ignored: " +
                            "id=${device.id} " +
                            "address=${device.address}"
                )
            }

            /*
             * I2S / Line / Bus Candidateを探す
             */
            val selectedDevice =
                findI2sInputDevice(
                    inputDevices
                )

            /*
             * preferredDevice設定
             */
            if (selectedDevice != null) {

                Log.d(
                    TAG,
                    "========================================"
                )

                Log.d(
                    TAG,
                    "I2S candidate selected:"
                )

                Log.d(
                    TAG,
                    "id=${selectedDevice.id}"
                )

                Log.d(
                    TAG,
                    "type=${selectedDevice.type}"
                )

                Log.d(
                    TAG,
                    "typeName=${
                        audioDeviceTypeName(
                            selectedDevice.type
                        )
                    }"
                )

                Log.d(
                    TAG,
                    "name=${selectedDevice.productName}"
                )

                Log.d(
                    TAG,
                    "address=${selectedDevice.address}"
                )

                val result =
                    rec.setPreferredDevice(
                        selectedDevice
                    )

                Log.d(
                    TAG,
                    "setPreferredDevice result=$result"
                )

                if (result) {

                    runOnUiThread {

                        status.text =
                            "Input request: ${
                                audioDeviceTypeName(
                                    selectedDevice.type
                                )
                            }"
                    }

                } else {

                    Log.e(
                        TAG,
                        "setPreferredDevice FAILED"
                    )

                    runOnUiThread {
                        status.text =
                            "I2S route request failed"
                    }
                }

            } else {

                Log.e(
                    TAG,
                    "========================================"
                )

                Log.e(
                    TAG,
                    "NO I2S / LINE / AUX / BUS INPUT"
                )

                Log.e(
                    TAG,
                    "Android Audio Policy does NOT expose I2S"
                )

                Log.e(
                    TAG,
                    "Default input will be used"
                )

                Log.e(
                    TAG,
                    "========================================"
                )

                runOnUiThread {
                    status.text =
                        "I2S NOT EXPOSED - using default input"
                }
            }

            /*
             * FFT / Software NR
             */
            val reducer =
                SpectralNoiseReducer(
                    FFT_SIZE
                )

            val buf =
                ShortArray(
                    FFT_SIZE
                )

            var smoothRaw =
                FloatArray(
                    FFT_SIZE / 2
                ) {
                    -120f
                }

            var smoothNr =
                smoothRaw.copyOf()

            /*
             * Recording Start
             */
            try {

                rec.startRecording()

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "startRecording failed",
                    e
                )

                runOnUiThread {
                    status.text =
                        "startRecording failed"
                }

                rec.release()

                running.set(false)

                return@Thread
            }

            Log.d(
                TAG,
                "========================================"
            )

            Log.d(
                TAG,
                "Recording started"
            )

            /*
             * 実際にAudioRecordが使用している
             * Physical Deviceを確認
             */
            Thread.sleep(100)

            val routedDevice =
                rec.routedDevice

            if (routedDevice != null) {

                Log.d(
                    TAG,
                    "========================================"
                )

                Log.d(
                    TAG,
                    "ACTUAL ROUTED DEVICE"
                )

                Log.d(
                    TAG,
                    "id=${routedDevice.id}"
                )

                Log.d(
                    TAG,
                    "type=${routedDevice.type}"
                )

                Log.d(
                    TAG,
                    "typeName=${
                        audioDeviceTypeName(
                            routedDevice.type
                        )
                    }"
                )

                Log.d(
                    TAG,
                    "name=${routedDevice.productName}"
                )

                Log.d(
                    TAG,
                    "address=${routedDevice.address}"
                )

                Log.d(
                    TAG,
                    "========================================"
                )

                /*
                 * 内蔵MICへ落ちている場合は警告
                 */
                if (
                    routedDevice.type ==
                    AudioDeviceInfo.TYPE_BUILTIN_MIC
                ) {

                    Log.e(
                        TAG,
                        "WARNING: Audio is routed to BUILTIN_MIC"
                    )

                    Log.e(
                        TAG,
                        "I2S -> UL2 route is NOT active"
                    )

                    runOnUiThread {

                        status.text =
                            "WARNING: BUILTIN MIC selected"
                    }

                } else {

                    runOnUiThread {

                        status.text =
                            "ROUTE: ${
                                audioDeviceTypeName(
                                    routedDevice.type
                                )
                            }"
                    }
                }

            } else {

                Log.w(
                    TAG,
                    "ROUTED DEVICE = null"
                )
            }

            /*
             * Main Audio Loop
             */
            while (running.get()) {

                val n =
                    rec.read(
                        buf,
                        0,
                        buf.size,
                        AudioRecord.READ_BLOCKING
                    )

                if (n <= 0) {

                    Log.w(
                        TAG,
                        "AudioRecord read=$n"
                    )

                    continue
                }

                /*
                 * FFT Sizeへ合わせる
                 */
                val input =
                    if (n == FFT_SIZE) {

                        buf.copyOf()

                    } else {

                        ShortArray(
                            FFT_SIZE
                        ) {

                            if (it < n) {
                                buf[it]
                            } else {
                                0
                            }
                        }
                    }

                /*
                 * Software Noise Reduction
                 */
                val out =
                    reducer.process(
                        input
                    )

                /*
                 * RAW FFT
                 */
                val rawSpectrum =
                    Fft.spectrumDb(
                        input,
                        FFT_SIZE
                    )

                /*
                 * NR FFT
                 */
                val nrSpectrum =
                    Fft.spectrumDb(
                        out,
                        FFT_SIZE
                    )

                /*
                 * Spectrum smoothing
                 */
                for (i in rawSpectrum.indices) {

                    smoothRaw[i] =
                        0.72f *
                                smoothRaw[i] +
                                0.28f *
                                rawSpectrum[i]

                    smoothNr[i] =
                        0.72f *
                                smoothNr[i] +
                                0.28f *
                                nrSpectrum[i]
                }

                /*
                 * SpectrumViewへFFTデータ転送
                 */
                spectrum.raw =
                    smoothRaw.copyOf()

                spectrum.nr =
                    smoothNr.copyOf()

                spectrum.postInvalidateOnAnimation()

                /*
                 * RMS
                 */
                val rawRms =
                    rmsDb(
                        input
                    )

                val nrRms =
                    rmsDb(
                        out
                    )

                /*
                 * Status表示
                 */
                runOnUiThread {

                    status.text =
                        "RAW %.1f dBFS  NR %.1f dBFS  Δ %.1f dB".format(
                            rawRms,
                            nrRms,
                            rawRms - nrRms
                        )
                }
            }

            /*
             * Stop
             */
            runCatching {

                if (
                    rec.recordingState ==
                    AudioRecord.RECORDSTATE_RECORDING
                ) {

                    rec.stop()
                }
            }

            rec.release()

            Log.d(
                TAG,
                "Recording stopped"
            )

            Log.d(
                TAG,
                "========================================"
            )

        }.also {

            it.start()
        }
    }

    /*
     * RMS -> dBFS
     */
    private fun rmsDb(
        samples: ShortArray
    ): Double {

        if (samples.isEmpty()) {
            return -120.0
        }

        var sum = 0.0

        for (sample in samples) {

            val value =
                sample / 32768.0

            sum +=
                value * value
        }

        val rms =
            sqrt(
                sum /
                        samples.size
            )

        return 20.0 *
                log10(
                    max(
                        rms,
                        1e-7
                    )
                )
    }

    /*
     * Recording Stop
     */
    private fun stopAudio() {

        running.set(false)

        worker?.interrupt()

        worker = null

        status.text =
            "Stopped"
    }

    override fun onStop() {

        super.onStop()

        stopAudio()
    }
}