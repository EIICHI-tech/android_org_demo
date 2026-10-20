package com.example.g510yolo

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

class YoloV8Detector(context: Context) {

    private val labels = context.assets.open("labels.txt").bufferedReader().readLines()
    private val interpreter: Interpreter
    private val inputSize = 640

    init {
        val modelBytes = context.assets.open("yolov8n_float32.tflite").readBytes()
        val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size).order(ByteOrder.nativeOrder())
        modelBuffer.put(modelBytes)
        modelBuffer.rewind()

        interpreter = Interpreter(
            modelBuffer,
            Interpreter.Options().apply { setNumThreads(4) }
        )

        val shape = interpreter.getOutputTensor(0).shape()
        require(shape.contentEquals(intArrayOf(1, 84, 8400))) {
            "Unsupported output ${shape.contentToString()}"
        }
    }

    fun detect(source: Bitmap, threshold: Float = 0.30f): List<Detection> {
        val bitmap = Bitmap.createScaledBitmap(source, inputSize, inputSize, true)
        val input = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4)
            .order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        pixels.forEach { pixel ->
            input.putFloat(((pixel shr 16) and 255) / 255f)
            input.putFloat(((pixel shr 8) and 255) / 255f)
            input.putFloat((pixel and 255) / 255f)
        }
        input.rewind()

        val output = Array(1) { Array(84) { FloatArray(8400) } }
        interpreter.run(input, output)

        val candidates = mutableListOf<Detection>()

        for (index in 0 until 8400) {
            var classIndex = -1
            var bestScore = 0f

            for (channel in 4 until 84) {
                if (output[0][channel][index] > bestScore) {
                    bestScore = output[0][channel][index]
                    classIndex = channel - 4
                }
            }

            if (bestScore < threshold || classIndex !in labels.indices) continue
            if (labels[classIndex] == "person") continue

            val centerX = output[0][0][index] / inputSize
            val centerY = output[0][1][index] / inputSize
            val width = output[0][2][index] / inputSize
            val height = output[0][3][index] / inputSize

            candidates += Detection(
                label = labels[classIndex],
                score = bestScore,
                left = (centerX - width / 2).coerceIn(0f, 1f),
                top = (centerY - height / 2).coerceIn(0f, 1f),
                right = (centerX + width / 2).coerceIn(0f, 1f),
                bottom = (centerY + height / 2).coerceIn(0f, 1f)
            )
        }

        return nonMaximumSuppression(candidates).take(10)
    }

    private fun nonMaximumSuppression(items: List<Detection>): List<Detection> {
        val sorted = items.sortedByDescending { it.score }.toMutableList()
        val kept = mutableListOf<Detection>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            kept += best
            sorted.removeAll { intersectionOverUnion(best, it) > 0.45f }
        }

        return kept
    }

    private fun intersectionOverUnion(a: Detection, b: Detection): Float {
        val left = max(a.left, b.left)
        val top = max(a.top, b.top)
        val right = min(a.right, b.right)
        val bottom = min(a.bottom, b.bottom)

        val intersection = max(0f, right - left) * max(0f, bottom - top)
        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)

        return intersection / (areaA + areaB - intersection + 1e-6f)
    }

    fun close() {
        interpreter.close()
    }
}
