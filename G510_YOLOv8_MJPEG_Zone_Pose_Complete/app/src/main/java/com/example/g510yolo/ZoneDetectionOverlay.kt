package com.example.g510yolo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

class ZoneDetectionOverlay(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class Zone {
        LEFT,
        TARGET,
        RIGHT
    }

    private var detections: List<Detection> = emptyList()

    private val fillPaint = Paint()
    private val linePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 30f
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(4f, 1f, 1f, Color.BLACK)
    }

    fun update(items: List<Detection>) {
        detections = items
        invalidate()
    }

    fun zoneOf(detection: Detection): Zone {
        return when {
            detection.centerX < 0.33f -> Zone.LEFT
            detection.centerX > 0.67f -> Zone.RIGHT
            else -> Zone.TARGET
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        fillPaint.color = 0x332196F3
        canvas.drawRect(0f, 0f, w * 0.33f, h, fillPaint)

        fillPaint.color = 0x334CAF50
        canvas.drawRect(w * 0.33f, 0f, w * 0.67f, h, fillPaint)

        fillPaint.color = 0x33FF9800
        canvas.drawRect(w * 0.67f, 0f, w, h, fillPaint)

        linePaint.color = Color.WHITE
        canvas.drawLine(w * 0.33f, 0f, w * 0.33f, h, linePaint)
        canvas.drawLine(w * 0.67f, 0f, w * 0.67f, h, linePaint)

        canvas.drawText("LEFT", 20f, 42f, textPaint)
        canvas.drawText("TARGET", w * 0.43f, 42f, textPaint)
        canvas.drawText("RIGHT", w * 0.75f, 42f, textPaint)

        detections.forEach { detection ->
            val zone = zoneOf(detection)
            linePaint.color = when (zone) {
                Zone.LEFT -> Color.CYAN
                Zone.TARGET -> Color.GREEN
                Zone.RIGHT -> Color.YELLOW
            }

            val rect = RectF(
                detection.left * w,
                detection.top * h,
                detection.right * w,
                detection.bottom * h
            )

            canvas.drawRect(rect, linePaint)
            canvas.drawCircle(
                detection.centerX * w,
                detection.centerY * h,
                8f,
                linePaint
            )

            val label = "${detection.label} ${(detection.score * 100).toInt()}% ${zone.name}"
            canvas.drawText(label, rect.left, rect.top.coerceAtLeast(80f), textPaint)
        }
    }
}
