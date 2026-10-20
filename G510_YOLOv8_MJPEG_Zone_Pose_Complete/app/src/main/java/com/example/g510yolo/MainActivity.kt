package com.example.g510yolo

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : Activity() {

    companion object {
        private const val TAG = "G510_AUTO"
        private const val PREFS_NAME = "cfg"
        private const val SCAN_END_ANGLE = 30
        private const val SCAN_STEP = 3

        // カメラ画像は毎回表示し、YOLO推論だけ3フレームに1回実行します。
        private const val DETECTION_INTERVAL = 3
    }

    private lateinit var img: ImageView
    private lateinit var ov: ZoneDetectionOverlay
    private lateinit var status: TextView
    private lateinit var poseText: TextView
    private lateinit var detText: TextView

    private val io = Executors.newSingleThreadExecutor()
    private val stopIo = Executors.newSingleThreadExecutor()
    private val running = AtomicBoolean(false)

    private var model: YoloV8Detector? = null
    private var cam = "http://172.20.10.14/stream"
    private var robotUrl = "http://172.20.10.13"
    private lateinit var robot: RobotClient

    private var current = Pose(90, 120, 90, 35)
    private var selected = "HOME"

    private val presets = linkedMapOf(
        "HOME" to Pose(90, 120, 90, 35),
        "SEARCH" to Pose(150, 105, 75, 35),
        "PICK" to Pose(90, 135, 105, 35),
        "GRIP" to Pose(90, 135, 105, 80),
        "LIFT" to Pose(90, 110, 85, 80),
        "DROP_ABOVE" to Pose(150, 110, 85, 80),
        "DROP_DOWN" to Pose(150, 135, 105, 80),
        "RELEASE" to Pose(150, 135, 105, 35)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        loadSettings()
        robot = RobotClient(robotUrl)
        loadModel()
        bindButtons()
        updatePoseText()
    }

    private fun bindViews() {
        img = findViewById(R.id.cameraImage)
        ov = findViewById(R.id.overlay)
        status = findViewById(R.id.stateText)
        poseText = findViewById(R.id.poseText)
        detText = findViewById(R.id.detectionText)
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        cam = prefs.getString("cam", cam) ?: cam
        robotUrl = prefs.getString("robot", robotUrl) ?: robotUrl

        presets.keys.toList().forEach { key ->
            val defaultPose = presets.getValue(key)
            presets[key] = Pose(
                prefs.getInt("${key}a", defaultPose.a),
                prefs.getInt("${key}b", defaultPose.b),
                prefs.getInt("${key}c", defaultPose.c),
                prefs.getInt("${key}d", defaultPose.d)
            )
        }
    }

    private fun loadModel() {
        try {
            model = YoloV8Detector(this)
            show("YOLO MODEL OK")
            Log.d(TAG, "YOLO model loaded")
        } catch (e: Exception) {
            model = null
            show("MODEL ERROR: ${e.message}")
            Log.e(TAG, "Model load failed", e)
        }
    }

    private fun bindButtons() {
        bindAdjust(R.id.aMinus, 'a', -5)
        bindAdjust(R.id.aPlus, 'a', 5)
        bindAdjust(R.id.bMinus, 'b', -5)
        bindAdjust(R.id.bPlus, 'b', 5)
        bindAdjust(R.id.cMinus, 'c', -5)
        bindAdjust(R.id.cPlus, 'c', 5)
        bindAdjust(R.id.dMinus, 'd', -5)
        bindAdjust(R.id.dPlus, 'd', 5)

        findViewById<Button>(R.id.executePose).setOnClickListener {
            executePose(current, "CURRENT")
        }

        bindPreset(R.id.poseHome, "HOME")
        bindPreset(R.id.poseSearch, "SEARCH")
        bindPreset(R.id.posePick, "PICK")
        bindPreset(R.id.poseGrip, "GRIP")
        bindPreset(R.id.poseLift, "LIFT")
        bindPreset(R.id.poseDropAbove, "DROP_ABOVE")
        bindPreset(R.id.poseDropDown, "DROP_DOWN")
        bindPreset(R.id.poseRelease, "RELEASE")

        findViewById<Button>(R.id.saveCurrent).setOnClickListener {
            presets[selected] = current.copy()

            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt("${selected}a", current.a)
                .putInt("${selected}b", current.b)
                .putInt("${selected}c", current.c)
                .putInt("${selected}d", current.d)
                .apply()

            show("SAVED $selected ${current.text()}")
        }

        findViewById<Button>(R.id.testCameraButton).setOnClickListener {
            show("CAMERA CONNECTING")
            io.execute {
                safe("CAMERA TEST") {
                    testCamera()
                }
            }
        }

        findViewById<Button>(R.id.testRobotButton).setOnClickListener {
            show("eArm CONNECTING")
            io.execute {
                val result = robot.health()
                show(if (result.ok) "eArm OK" else "eArm NG: ${result.message}")
            }
        }

        findViewById<Button>(R.id.startButton).setOnClickListener {
            show("START BUTTON")
            Log.d(TAG, "START button clicked")

            if (running.get()) {
                show("ALREADY RUNNING")
                return@setOnClickListener
            }

            val detector = model
            if (detector == null) {
                show("YOLO MODEL NOT LOADED")
                return@setOnClickListener
            }

            running.set(true)
            show("AUTO START")

            io.execute {
                safe("AUTO") {
                    auto(detector)
                }
            }
        }

        findViewById<Button>(R.id.emergencyButton).setOnClickListener {
            running.set(false)
            show("EMERGENCY STOP")
            stopIo.execute {
                robot.stop()
            }
        }

        findViewById<Button>(R.id.settingsButton).setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun bindAdjust(id: Int, axis: Char, delta: Int) {
        findViewById<Button>(id).setOnClickListener {
            current = when (axis) {
                'a' -> current.copy(a = (current.a + delta).coerceIn(0, 180))
                'b' -> current.copy(b = (current.b + delta).coerceIn(0, 180))
                'c' -> current.copy(c = (current.c + delta).coerceIn(0, 180))
                else -> current.copy(d = (current.d + delta).coerceIn(0, 180))
            }
            updatePoseText()
        }
    }

    private fun bindPreset(id: Int, name: String) {
        findViewById<Button>(id).setOnClickListener {
            selected = name
            current = presets.getValue(name).copy()
            updatePoseText()
            executePose(current, name)
        }
    }

    private fun updatePoseText() {
        poseText.text = "$selected  ${current.text()}"
    }

    private fun show(message: String) {
        Log.d(TAG, message)
        runOnUiThread {
            status.text = message
        }
    }

    private fun safe(name: String, task: () -> Unit) {
        try {
            task()
        } catch (e: Exception) {
            running.set(false)
            show("ERROR $name: ${e.message}")
            Log.e(TAG, "$name failed", e)
        }
    }

    private fun executePose(pose: Pose, name: String) {
        io.execute {
            safe("POSE $name") {
                show("MOVING $name")
                val result = robot.pose(pose)
                if (!result.ok) {
                    error(result.message)
                }
                show("DONE $name ${pose.text()}")
            }
        }
    }

    private fun testCamera() {
        MjpegFrameReader(cam).use { reader ->
            reader.open()

            val bitmap = reader.nextFrame()
                ?: error("MJPEG frame decode failed")

            val detections = model?.detect(bitmap).orEmpty()

            runOnUiThread {
                img.setImageBitmap(bitmap)
                ov.update(detections)
                detText.text = detectionSummary(detections)
            }

            show("CAMERA / ZONE OK")
        }
    }

    private fun detectionSummary(detections: List<Detection>): String {
        if (detections.isEmpty()) {
            return "NO DETECT"
        }
        else {
            return "DETECTED"
        }
        //val best = detections.first()
        //val zone = ov.zoneOf(best)

        //return "${best.label} ${(best.score * 100).toInt()}% ${zone.name}"
    }

    /**
     * LEFT/TARGET/RIGHTを画面には表示しますが、把持開始条件には使いません。
     * YOLOが物体を1個でも返した時点でスキャンを終了し、把持シーケンスへ進みます。
     */
    private fun auto(detector: YoloV8Detector) {
        try {
            show("AUTO ENTER")

            val searchPose = presets.getValue("SEARCH")
            val initialResult = robot.pose(searchPose)
            if (!initialResult.ok) {
                error("SEARCH通信失敗: ${initialResult.message}")
            }

            MjpegFrameReader(cam).use { reader ->
                reader.open()

                var frameCounter = 0
                var foundAngle: Int? = null

                for (angle in searchPose.a downTo SCAN_END_ANGLE step SCAN_STEP) {
                    if (!running.get()) {
                        show("AUTO CANCELLED")
                        return
                    }

                    show("SCAN A=$angle")

                    val scanPose = searchPose.copy(a = angle)
                    val moveResult = robot.pose(scanPose)
                    if (!moveResult.ok) {
                        error("SCAN通信失敗: ${moveResult.message}")
                    }

                    val bitmap = reader.nextFrame()
                    if (bitmap == null) {
                        continue
                    }

                    frameCounter++

                    // 画像はすべて表示します。
                    runOnUiThread {
                        img.setImageBitmap(bitmap)
                    }

                    // YOLO推論は指定間隔のフレームだけで実行します。
                    if (frameCounter % DETECTION_INTERVAL != 0) {
                        continue
                    }

                    val detections = detector.detect(bitmap)

                    runOnUiThread {
                        ov.update(detections)
                        detText.text = detectionSummary(detections)
                    }

                    Log.d(
                        TAG,
                        "inference frame=$frameCounter detections=${detections.size}"
                    )

                    // エリアや連続検出回数に関係なく、
                    // 何か1個でも検出したらその場で把持へ進みます。
                    if (detections.isNotEmpty()) {
                        val first = detections.first()

                        foundAngle = angle

                        show(
                            "FOUND OBJECT ${first.label} " +
                                    "${(first.score * 100).toInt()}% A=$angle"
                        )

                        break
                    }
                }

                if (foundAngle == null) {
                    robot.pose(presets.getValue("HOME"))
                    show("NOT FOUND")
                    return
                }

                runPickAndPlace(foundAngle)
                show("COMPLETED")
            }
        } finally {
            running.set(false)
        }
    }

    private fun runPickAndPlace(foundAngle: Int) {
        val sequence = listOf(
            "PICK",
            "GRIP",
            "LIFT",
            "DROP_ABOVE",
            "DROP_DOWN",
            "RELEASE",
            "HOME"
        )

        for (name in sequence) {
            if (!running.get()) {
                show("SEQUENCE CANCELLED")
                return
            }

            val savedPose = presets.getValue(name)

            val targetPose = if (name in listOf("PICK", "GRIP", "LIFT")) {
                savedPose.copy(a = foundAngle)
            } else {
                savedPose
            }

            show(name)

            val result = robot.pose(targetPose)
            if (!result.ok) {
                error("$name 通信失敗: ${result.message}")
            }

            // 画像表示が追いつくよう、各Poseの間を少し長くしています。
            Thread.sleep(800)
        }
    }

    private fun showSettingsDialog() {
        val robotInput = EditText(this).apply {
            setText(robotUrl)
            hint = "eArm URL"
        }

        val cameraInput = EditText(this).apply {
            setText(cam)
            hint = "MJPEG URL"
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 0)
            addView(robotInput)
            addView(cameraInput)
        }

        AlertDialog.Builder(this)
            .setTitle("接続設定")
            .setView(box)
            .setPositiveButton("保存") { _, _ ->
                robotUrl = robotInput.text.toString().trim()
                cam = cameraInput.text.toString().trim()
                robot.base = robotUrl

                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString("robot", robotUrl)
                    .putString("cam", cam)
                    .apply()

                show("SETTINGS SAVED")
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onPause() {
        running.set(false)
        stopIo.execute {
            robot.stop()
        }
        super.onPause()
    }

    override fun onDestroy() {
        running.set(false)
        model?.close()
        io.shutdownNow()
        stopIo.shutdownNow()
        super.onDestroy()
    }
}
