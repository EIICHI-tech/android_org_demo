package jp.co.demo.voicellmchat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LlmClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    suspend fun streamChat(
        endpoint: String,
        apiKey: String,
        model: String,
        history: List<ChatMessage>,
        onToken: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        require(endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            "Endpointは http:// または https:// から入力してください"
        }

        val messages = JSONArray().put(
            JSONObject()
                .put("role", "system")
                .put("content", "あなたは日本語で簡潔かつ親切に会話する音声アシスタントです。読み上げやすく、原則3文以内で回答してください。")
        )
        history.takeLast(12).forEach {
            messages.put(JSONObject().put("role", it.role).put("content", it.text))
        }

        val json = JSONObject()
            .put("model", model)
            .put("stream", true)
            .put("temperature", 0.6)
            .put("messages", messages)

        val requestBuilder = Request.Builder()
            .url(endpoint.trimEnd('/') + "/v1/chat/completions")
            .header("Accept", "text/event-stream")
            .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        if (apiKey.isNotBlank()) requestBuilder.header("Authorization", "Bearer $apiKey")

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code}: ${response.body?.string()?.take(400).orEmpty()}")
            }
            val source = response.body?.source() ?: error("LLMの応答本文がありません")
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") break
                runCatching {
                    JSONObject(data).getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("delta").optString("content")
                }.getOrNull()?.takeIf(String::isNotEmpty)?.let(onToken)
            }
        }
    }
}
