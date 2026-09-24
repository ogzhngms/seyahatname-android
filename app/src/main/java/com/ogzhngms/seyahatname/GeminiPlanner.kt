package com.ogzhngms.seyahatname

import androidx.annotation.StringRes
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

// Free-tier models that answer with schema JSON, best first. Each has its own capacity and quota,
// so when one cannot serve (retired, rate-limited or overloaded) the request moves down this list.
enum class AiModel(val id: String, val label: String) {
    GEMINI_3_8_FLASH("gemini-3.8-flash", "Gemini 3.8 Flash"),
    GEMINI_3_7_FLASH("gemini-3.7-flash", "Gemini 3.7 Flash"),
    GEMINI_3_6_FLASH("gemini-3.6-flash", "Gemini 3.6 Flash"),
    GEMINI_3_5_FLASH("gemini-3.5-flash", "Gemini 3.5 Flash"),
    GEMINI_3_5_FLASH_LITE("gemini-3.5-flash-lite", "Gemini 3.5 Flash-Lite"),
    GEMINI_3_1_FLASH_LITE("gemini-3.1-flash-lite", "Gemini 3.1 Flash-Lite"),
    GEMMA_4_31B("gemma-4-31b-it", "Gemma 4 31B"),
    GEMMA_4_26B("gemma-4-26b-a4b-it", "Gemma 4 26B"),
}

// The plan JSON and the name of the model that actually wrote it.
data class PlanReply(val json: String, val model: String)

class PlanException(@StringRes val messageRes: Int, val detail: String? = null) : Exception(detail)

// Calls the Gemini REST API with the key from local.properties. baseUrl is only changed by tests.
class GeminiPlanner(
    private val apiKey: String,
    private val baseUrl: String = "https://generativelanguage.googleapis.com",
) {
    // Tries the chosen model first, then the others in list order until one answers.
    suspend fun plan(prompt: String, first: AiModel): PlanReply = withContext(Dispatchers.IO) {
        val request = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT))))
            .put(
                "contents",
                JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", prompt)))),
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("responseMimeType", "application/json")
                    .put("responseJsonSchema", JSONObject(ITINERARY_SCHEMA)),
            )
        val order = listOf(first) + (AiModel.entries - first)
        var model = order.first()
        var reply = post(model, request)
        for (next in order.drop(1)) {
            if (!cannotServe(reply.first)) break
            model = next
            reply = post(model, request)
        }
        val (status, body) = reply
        if (status !in 200..299) throw PlanException(errorFor(status, body), "${model.label} · HTTP $status: ${body.take(300)}")
        PlanReply(planText(JSONObject(body)), model.label)
    }

    private fun post(model: AiModel, request: JSONObject): Pair<Int, String> {
        val connection = URL("$baseUrl/v1beta/models/${model.id}:generateContent").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 120_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-goog-api-key", apiKey)
            connection.outputStream.use { it.write(request.toString().toByteArray()) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            return status to stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        } finally {
            connection.disconnect()
        }
    }
}

// Retired (404), out of free quota (429) or overloaded (5xx): another model may still answer.
private fun cannotServe(status: Int) = status == 404 || status == 429 || status >= 500

// The answer's text parts joined; thinking parts are skipped.
internal fun planText(response: JSONObject): String {
    // No candidate at all means the prompt itself was blocked.
    val candidate = response.optJSONArray("candidates")?.optJSONObject(0) ?: throw PlanException(R.string.error_refusal)
    when (candidate.optString("finishReason")) {
        "", "STOP" -> Unit
        "MAX_TOKENS" -> throw PlanException(R.string.error_too_long)
        else -> throw PlanException(R.string.error_refusal)
    }
    val parts = candidate.getJSONObject("content").getJSONArray("parts")
    return List(parts.length()) { parts.getJSONObject(it) }
        .filterNot { it.optBoolean("thought") }
        .joinToString("") { it.optString("text") }
}

@StringRes
private fun errorFor(status: Int, body: String): Int = when {
    status == 429 -> R.string.error_rate_limit
    status == 503 -> R.string.error_busy
    // A wrong key comes back as 400 with reason API_KEY_INVALID.
    status == 401 || status == 403 || "API_KEY_INVALID" in body -> R.string.error_api_key
    else -> R.string.error_service
}

@StringRes
fun errorMessage(error: Throwable): Int = when (error) {
    is PlanException -> error.messageRes
    is IOException -> R.string.error_network
    is JSONException -> R.string.error_parse
    else -> R.string.error_generic
}

// The API's own answer, shown under the friendly text so a failing key or quota is easy to diagnose.
fun errorDetail(error: Throwable): String? = (error as? PlanException)?.detail
