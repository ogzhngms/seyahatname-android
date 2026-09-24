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

// Preferred model first. Demand spikes hit models separately, so on a 5xx the request is re-run on the next one.
val GEMINI_MODELS = listOf("gemini-3.8-flash", "gemini-3.5-flash")

class PlanException(@StringRes val messageRes: Int, val detail: String? = null) : Exception(detail)

// Calls the Gemini REST API with the key from local.properties. baseUrl is only changed by tests.
class GeminiPlanner(
    private val apiKey: String,
    private val baseUrl: String = "https://generativelanguage.googleapis.com",
) {
    // Returns the plan exactly as Gemini wrote it: JSON that follows ITINERARY_SCHEMA.
    suspend fun plan(prompt: String): String = withContext(Dispatchers.IO) {
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
        var reply = post(GEMINI_MODELS[0], request)
        for (model in GEMINI_MODELS.drop(1)) if (reply.first >= 500) reply = post(model, request)
        val (status, body) = reply
        if (status !in 200..299) throw PlanException(errorFor(status, body), "HTTP $status: ${body.take(300)}")
        planText(JSONObject(body))
    }

    private fun post(model: String, request: JSONObject): Pair<Int, String> {
        val connection = URL("$baseUrl/v1beta/models/$model:generateContent").openConnection() as HttpURLConnection
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
