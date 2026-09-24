package com.ogzhngms.seyahatname

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

// Runs the real SDK against a local fake of the Messages API: checks what the app sends and how it reads replies.
class ClaudePlannerTest {
    private val sample = File("src/main/res/raw/sample_itinerary.json").readText()

    @Volatile private var status = 200
    @Volatile private var reply = ""
    @Volatile private var sentBody = JSONObject()
    @Volatile private var sentBeta = ""

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/v1/messages") { exchange ->
            sentBeta = exchange.requestHeaders.getFirst("anthropic-beta").orEmpty()
            sentBody = JSONObject(exchange.requestBody.bufferedReader().readText())
            val bytes = reply.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        start()
    }
    private val planner = ClaudePlanner("test-key", "http://127.0.0.1:${server.address.port}")

    @After
    fun stop() = server.stop(0)

    @Test
    fun requestAsksOpusForSchemaJsonWithRefusalFallback() {
        reply = message("end_turn", sample)

        val json = runBlocking { planner.plan("Destination: Rome") }

        assertEquals(sample, json)
        assertEquals("claude-opus-5", sentBody.getString("model"))
        assertEquals("default", sentBody.getString("fallbacks"))
        assertTrue(sentBeta.contains("server-side-fallback-2026-07-01"))
        val format = sentBody.getJSONObject("output_config").getJSONObject("format")
        assertEquals("json_schema", format.getString("type"))
        assertEquals(ITINERARY_SCHEMA, plain(format.getJSONObject("schema")))
        assertEquals("Destination: Rome", sentBody.getJSONArray("messages").getJSONObject(0).getString("content"))
    }

    @Test
    fun refusalBecomesAFriendlyError() {
        reply = message("refusal", text = null)
        assertEquals(R.string.error_refusal, failure())
    }

    @Test
    fun truncatedPlanIsNotParsed() {
        reply = message("max_tokens", sample.take(100))
        assertEquals(R.string.error_too_long, failure())
    }

    @Test
    fun rejectedKeyPointsAtLocalProperties() {
        status = 401
        reply = """{"type": "error", "error": {"type": "authentication_error", "message": "invalid x-api-key"}}"""
        assertEquals(R.string.error_api_key, failure())
    }

    private fun failure(): Int = try {
        runBlocking { planner.plan("Destination: Rome") }
        fail("plan() should have thrown")
        0
    } catch (e: Exception) {
        errorMessage(e)
    }

    private fun message(stopReason: String, text: String?) = JSONObject()
        .put("id", "msg_test")
        .put("type", "message")
        .put("role", "assistant")
        .put("model", "claude-opus-5")
        .put("content", JSONArray().apply { if (text != null) put(JSONObject().put("type", "text").put("text", text)) })
        .put("stop_reason", stopReason)
        .put("stop_sequence", JSONObject.NULL)
        .put("usage", JSONObject().put("input_tokens", 1).put("output_tokens", 1))
        .toString()

    private fun plain(value: Any?): Any? = when (value) {
        is JSONObject -> value.keys().asSequence().associateWith { plain(value.get(it)) }
        is JSONArray -> List(value.length()) { plain(value.get(it)) }
        else -> value
    }
}
