package com.ogzhngms.seyahatname

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

// Runs GeminiPlanner against a local fake of the Gemini API: checks what the app sends and how it reads replies.
class GeminiPlannerTest {
    private val sample = File("src/main/res/raw/sample_itinerary.json").readText()

    @Volatile private var status = 200
    @Volatile private var reply = ""
    @Volatile private var sentPath = ""
    @Volatile private var sentKey = ""
    @Volatile private var sentBody = JSONObject()
    @Volatile private var overloaded = ""
    private val paths = mutableListOf<String>()

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/v1beta/models/") { exchange ->
            sentPath = exchange.requestURI.path
            synchronized(paths) { paths += sentPath }
            sentKey = exchange.requestHeaders.getFirst("x-goog-api-key").orEmpty()
            sentBody = JSONObject(exchange.requestBody.bufferedReader().readText())
            val busy = overloaded.isNotEmpty() && sentPath.contains("/$overloaded:")
            val bytes = (if (busy) """{"error": {"code": 503, "status": "UNAVAILABLE"}}""" else reply).toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(if (busy) 503 else status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        start()
    }
    private val planner = GeminiPlanner("test-key", "http://127.0.0.1:${server.address.port}")

    @After
    fun stop() = server.stop(0)

    @Test
    fun requestAsksForSchemaJsonAndSkipsThinkingParts() {
        reply = candidate("STOP", JSONObject().put("text", "planning…").put("thought", true), JSONObject().put("text", sample))

        val json = runBlocking { planner.plan("Destination: Rome") }

        assertEquals(sample, json)
        assertEquals("/v1beta/models/${GEMINI_MODELS[0]}:generateContent", sentPath)
        assertEquals("test-key", sentKey)
        val config = sentBody.getJSONObject("generationConfig")
        assertEquals("application/json", config.getString("responseMimeType"))
        assertEquals(ITINERARY_SCHEMA, plain(config.getJSONObject("responseJsonSchema")))
        assertEquals(SYSTEM_PROMPT, sentBody.getJSONObject("systemInstruction").getJSONArray("parts").getJSONObject(0).getString("text"))
        assertEquals("Destination: Rome", sentBody.getJSONArray("contents").getJSONObject(0).getJSONArray("parts").getJSONObject(0).getString("text"))
    }

    @Test
    fun overloadedModelFallsBackToTheNextOne() {
        overloaded = GEMINI_MODELS[0]
        reply = candidate("STOP", JSONObject().put("text", sample))

        assertEquals(sample, runBlocking { planner.plan("Destination: Rome") })
        assertEquals(GEMINI_MODELS.map { "/v1beta/models/$it:generateContent" }, synchronized(paths) { paths.toList() })
    }

    @Test
    fun everyModelOverloadedSaysTryLater() {
        status = 503
        reply = """{"error": {"code": 503, "status": "UNAVAILABLE"}}"""
        assertEquals(R.string.error_busy, failure())
        assertEquals(GEMINI_MODELS.size, synchronized(paths) { paths.size })
    }

    @Test
    fun blockedPromptBecomesAFriendlyError() {
        reply = JSONObject().put("promptFeedback", JSONObject().put("blockReason", "SAFETY")).toString()
        assertEquals(R.string.error_refusal, failure())
    }

    @Test
    fun truncatedPlanIsNotParsed() {
        reply = candidate("MAX_TOKENS", JSONObject().put("text", sample.take(100)))
        assertEquals(R.string.error_too_long, failure())
    }

    @Test
    fun wrongKeyPointsAtLocalProperties() {
        status = 400
        reply = """{"error": {"code": 400, "status": "INVALID_ARGUMENT", "details": [{"reason": "API_KEY_INVALID"}]}}"""
        assertEquals(R.string.error_api_key, failure())
    }

    @Test
    fun usedUpQuotaAsksToWait() {
        status = 429
        reply = """{"error": {"code": 429, "status": "RESOURCE_EXHAUSTED"}}"""
        assertEquals(R.string.error_rate_limit, failure())
    }

    private fun failure(): Int = try {
        runBlocking { planner.plan("Destination: Rome") }
        fail("plan() should have thrown")
        0
    } catch (e: Exception) {
        errorMessage(e)
    }

    private fun candidate(finishReason: String, vararg parts: JSONObject) = JSONObject()
        .put(
            "candidates",
            JSONArray().put(
                JSONObject()
                    .put("content", JSONObject().put("role", "model").put("parts", JSONArray(parts.toList())))
                    .put("finishReason", finishReason),
            ),
        )
        .toString()

    private fun plain(value: Any?): Any? = when (value) {
        is JSONObject -> value.keys().asSequence().associateWith { plain(value.get(it)) }
        is JSONArray -> List(value.length()) { plain(value.get(it)) }
        else -> value
    }
}
