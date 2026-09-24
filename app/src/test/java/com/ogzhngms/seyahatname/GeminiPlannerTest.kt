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
    @Volatile private var sentKey = ""
    @Volatile private var sentBody = JSONObject()
    @Volatile private var unavailable = emptyMap<AiModel, Int>()
    private val asked = mutableListOf<String>()

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/v1beta/models/") { exchange ->
            val model = exchange.requestURI.path.removePrefix("/v1beta/models/").removeSuffix(":generateContent")
            synchronized(asked) { asked += model }
            sentKey = exchange.requestHeaders.getFirst("x-goog-api-key").orEmpty()
            sentBody = JSONObject(exchange.requestBody.bufferedReader().readText())
            val down = unavailable.entries.firstOrNull { it.key.id == model }?.value
            val bytes = (if (down != null) """{"error": {"code": $down}}""" else reply).toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(down ?: status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        start()
    }
    private val planner = GeminiPlanner("test-key", "http://127.0.0.1:${server.address.port}")

    @After
    fun stop() = server.stop(0)

    private fun plan(first: AiModel = AiModel.GEMINI_3_8_FLASH) = runBlocking { planner.plan("Destination: Rome", first) }
    private fun asked() = synchronized(asked) { asked.toList() }

    @Test
    fun requestAsksForSchemaJsonAndSkipsThinkingParts() {
        reply = candidate("STOP", JSONObject().put("text", "planning…").put("thought", true), JSONObject().put("text", sample))

        assertEquals(PlanReply(sample, "Gemini 3.8 Flash"), plan())
        assertEquals(listOf("gemini-3.8-flash"), asked())
        assertEquals("test-key", sentKey)
        val config = sentBody.getJSONObject("generationConfig")
        assertEquals("application/json", config.getString("responseMimeType"))
        assertEquals(ITINERARY_SCHEMA, plain(config.getJSONObject("responseJsonSchema")))
        assertEquals(SYSTEM_PROMPT, sentBody.getJSONObject("systemInstruction").getJSONArray("parts").getJSONObject(0).getString("text"))
        assertEquals("Destination: Rome", sentBody.getJSONArray("contents").getJSONObject(0).getJSONArray("parts").getJSONObject(0).getString("text"))
    }

    @Test
    fun chosenModelIsAskedFirst() {
        reply = candidate("STOP", JSONObject().put("text", sample))
        assertEquals("Gemma 4 26B", plan(AiModel.GEMMA_4_26B).model)
        assertEquals(listOf("gemma-4-26b-a4b-it"), asked())
    }

    // Overloaded, out of free quota and retired models are skipped; the reply names the model that answered.
    @Test
    fun modelsThatCannotServeAreSkipped() {
        unavailable = mapOf(AiModel.GEMINI_3_8_FLASH to 503, AiModel.GEMINI_3_7_FLASH to 429, AiModel.GEMINI_3_6_FLASH to 404)
        reply = candidate("STOP", JSONObject().put("text", sample))

        assertEquals(PlanReply(sample, "Gemini 3.5 Flash"), plan())
        assertEquals(listOf("gemini-3.8-flash", "gemini-3.7-flash", "gemini-3.6-flash", "gemini-3.5-flash"), asked())
    }

    @Test
    fun everyModelOverloadedSaysTryLater() {
        status = 503
        reply = """{"error": {"code": 503, "status": "UNAVAILABLE"}}"""
        assertEquals(R.string.error_busy, failure())
        assertEquals(AiModel.entries.map { it.id }, asked())
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

    // A bad key fails the same way on every model, so there is no point asking the next one.
    @Test
    fun wrongKeyPointsAtLocalPropertiesWithoutTryingOtherModels() {
        status = 400
        reply = """{"error": {"code": 400, "status": "INVALID_ARGUMENT", "details": [{"reason": "API_KEY_INVALID"}]}}"""
        assertEquals(R.string.error_api_key, failure())
        assertEquals(1, asked().size)
    }

    private fun failure(): Int = try {
        plan()
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
