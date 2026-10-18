package jp.co.kagafei.g510pickplace

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min

class YoloV8Detector(private val context: Context) : AutoCloseable {
    private var interpreter: Interpreter? = null
    private val labels = context.assets.open("labels.txt").bufferedReader().readLines().filter { it.isNotBlank() }
    val ready: Boolean get() = interpreter != null
    var status: String = "モデル未読込"; private set

    init {
        try {
            val afd = context.assets.openFd("yolov8n_float32.tflite")
            val model = FileInputStream(afd.fileDescriptor).channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
            interpreter = Interpreter(model, Interpreter.Options().apply { setNumThreads(4); setUseXNNPACK(true) })
            status = "YOLOv8モデル読込済み"
        } catch (_: Exception) { status = "assets/yolov8n_float32.tflite を追加してください" }
    }

    fun detect(source: Bitmap, threshold: Float = .35f): List<Detection> {
        val t = interpreter ?: return emptyList()
        val resized = Bitmap.createScaledBitmap(source, 640, 640, true)
        val input = ByteBuffer.allocateDirect(1 * 640 * 640 * 3 * 4).order(ByteOrder.nativeOrder())
        val px = IntArray(640 * 640); resized.getPixels(px, 0, 640, 0, 0, 640, 640)
        px.forEach { p -> input.putFloat(((p shr 16) and 255) / 255f); input.putFloat(((p shr 8) and 255) / 255f); input.putFloat((p and 255) / 255f) }
        input.rewind()

        val shape = t.getOutputTensor(0).shape()
        val d1 = shape[1]; val d2 = shape[2]
        val output = Array(1) { Array(d1) { FloatArray(d2) } }
        t.run(input, output)
        val channelsFirst = d1 < d2
        val candidates = mutableListOf<Detection>()
        val count = if (channelsFirst) d2 else d1
        val attrs = if (channelsFirst) d1 else d2
        for (i in 0 until count) {
            fun v(a: Int) = if (channelsFirst) output[0][a][i] else output[0][i][a]
            var best = 0f; var cls = -1
            for (c in 4 until attrs) if (v(c) > best) { best = v(c); cls = c - 4 }
            if (best < threshold || cls !in labels.indices) continue
            val cx=v(0); val cy=v(1); val w=v(2); val h=v(3)
            candidates += Detection(labels[cls], best, RectF((cx-w/2)/640f,(cy-h/2)/640f,(cx+w/2)/640f,(cy+h/2)/640f))
        }
        return nms(candidates).take(20)
    }

    private fun nms(items: List<Detection>, iou: Float=.45f): List<Detection> {
        val pending=items.sortedByDescending{it.score}.toMutableList(); val out=mutableListOf<Detection>()
        while(pending.isNotEmpty()) { val a=pending.removeAt(0); out+=a; pending.removeAll { it.label==a.label && iou(a.box,it.box)>iou } }
        return out
    }
    private fun iou(a:RectF,b:RectF):Float { val x1=max(a.left,b.left);val y1=max(a.top,b.top);val x2=min(a.right,b.right);val y2=min(a.bottom,b.bottom);val inter=max(0f,x2-x1)*max(0f,y2-y1);return inter/(a.width()*a.height()+b.width()*b.height()-inter+1e-6f) }
    override fun close(){ interpreter?.close() }
}
