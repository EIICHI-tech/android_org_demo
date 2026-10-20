package com.example.g510yolo

import java.net.HttpURLConnection
import java.net.URL

data class Pose(
    val a: Int,
    val b: Int,
    val c: Int,
    val d: Int
) {
    fun text(): String = "A=$a B=$b C=$c D=$d"
}

class RobotClient(var base: String) {

    data class Result(
        val ok: Boolean,
        val code: Int,
        val message: String
    )

    fun health(): Result = request("/health", 1_500, 1_500)

    fun pose(pose: Pose): Result {
        return request(
            "/pose?a=${pose.a}&b=${pose.b}&c=${pose.c}&d=${pose.d}&speed=2",
            3_000,
            15_000
        )
    }

    fun stop(): Result = request("/stop", 1_000, 1_000)

    private fun request(path: String, connectMs: Int, readMs: Int): Result {
        var connection: HttpURLConnection? = null

        return try {
            connection = URL(base.trimEnd('/') + path).openConnection() as HttpURLConnection
            connection.connectTimeout = connectMs
            connection.readTimeout = readMs
            connection.useCaches = false
            connection.requestMethod = "GET"

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = try {
                stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            } catch (_: Exception) {
                ""
            }

            Result(code in 200..299, code, body.ifBlank { "HTTP $code" })
        } catch (e: Exception) {
            Result(false, -1, "${e.javaClass.simpleName}: ${e.message.orEmpty()}")
        } finally {
            connection?.disconnect()
        }
    }
}
