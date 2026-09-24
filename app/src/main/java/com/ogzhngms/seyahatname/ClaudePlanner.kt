package com.ogzhngms.seyahatname

import androidx.annotation.StringRes
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException

class PlanException(@StringRes val messageRes: Int) : Exception()

// baseUrl is only set by tests, which point the client at a local fake server.
class ClaudePlanner(apiKey: String, baseUrl: String? = null) {
    private val client = AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        .apply { if (baseUrl != null) baseUrl(baseUrl) }
        .build()

    // Returns the plan exactly as Claude wrote it: JSON that follows ITINERARY_SCHEMA.
    suspend fun plan(prompt: String): String = withContext(Dispatchers.IO) {
        val schema = JsonOutputFormat.Schema.builder()
            .putAllAdditionalProperties(ITINERARY_SCHEMA.mapValues { JsonValue.from(it.value) })
            .build()
        val params = MessageCreateParams.builder()
            .model("claude-opus-5")
            .maxTokens(16000L)
            .system(SYSTEM_PROMPT)
            .addUserMessage(prompt)
            .outputConfig(
                OutputConfig.builder()
                    // ponytail: MEDIUM keeps the wait near a minute; raise to HIGH if plans feel thin.
                    .effort(OutputConfig.Effort.MEDIUM)
                    .format(JsonOutputFormat.builder().schema(schema).build())
                    .build(),
            )
            // If Opus 5 declines, the API re-runs the request on Anthropic's recommended fallback model.
            .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
            .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
            .build()
        val message = client.messages().create(params)
        when (message.stopReason().orElse(null)) {
            StopReason.REFUSAL -> throw PlanException(R.string.error_refusal)
            StopReason.MAX_TOKENS -> throw PlanException(R.string.error_too_long)
            else -> Unit
        }
        message.content().mapNotNull { it.text().orElse(null)?.text() }.joinToString("")
    }
}

@StringRes
fun errorMessage(error: Throwable): Int = when (error) {
    is PlanException -> error.messageRes
    is UnauthorizedException, is PermissionDeniedException -> R.string.error_api_key
    is RateLimitException -> R.string.error_rate_limit
    is AnthropicServiceException -> if (error.statusCode() == 402) R.string.error_billing else R.string.error_service
    is AnthropicIoException -> R.string.error_network
    is JSONException -> R.string.error_parse
    else -> R.string.error_generic
}

// The API's own message, shown under the friendly text so a failing key or quota is easy to diagnose.
fun errorDetail(error: Throwable): String? = (error as? AnthropicServiceException)?.message
