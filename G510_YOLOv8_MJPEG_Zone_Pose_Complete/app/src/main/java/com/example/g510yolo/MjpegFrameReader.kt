package com.example.g510yolo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.EOFException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class MjpegFrameReader(
    private val url: String
) : Closeable {

    private var connection: HttpURLConnection? = null
    private var input: BufferedInputStream? = null

    fun open() {
        close()

        connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            useCaches = false
            doInput = true
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Connection", "Keep-Alive")
            connect()

            if (responseCode !in 200..299) {
                throw IOException("MJPEG HTTP $responseCode")
            }
        }

        input = BufferedInputStream(connection!!.inputStream, 64 * 1024)
        Log.d("MJPEG", "Connected: $url")
    }

    fun nextFrame(maxBytes: Int = 5_000_000): Bitmap? {
        val stream = input ?: throw IOException("MJPEG stream not opened")
        val jpeg = ByteArrayOutputStream(128 * 1024)

        var previous = -1
        var started = false

        while (jpeg.size() < maxBytes) {
            val current = stream.read()
            if (current < 0) {
                throw EOFException("MJPEG stream closed")
            }

            if (!started) {
                if (previous == 0xFF && current == 0xD8) {
                    jpeg.write(0xFF)
                    jpeg.write(0xD8)
                    started = true
                }
                previous = current
                continue
            }

            jpeg.write(current)

            if (previous == 0xFF && current == 0xD9) {
                val bytes = jpeg.toByteArray()
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }

            previous = current
        }

        throw IOException("MJPEG frame too large")
    }

    override fun close() {
        try {
            input?.close()
        } catch (_: Exception) {
        }

        try {
            connection?.disconnect()
        } catch (_: Exception) {
        }

        input = null
        connection = null
    }
}
