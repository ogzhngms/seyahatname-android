package com.ogzhngms.seyahatname

import java.io.File
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItineraryTest {
    private val samples = listOf("raw", "raw-tr").map { File("src/main/res/$it/sample_itinerary.json").readText() }

    @Test
    fun samplePlansParse() {
        samples.map(::parseItinerary).forEach { plan ->
            assertEquals(listOf(1, 2, 3), plan.days.map { it.day })
            assertTrue(plan.days.all { it.activities.isNotEmpty() })
            assertEquals(4, plan.tips.size)
        }
    }

    @Test
    fun samplePlansMatchTheSchemaClaudeIsGiven() {
        samples.forEach { assertMatches(ITINERARY_SCHEMA, JSONObject(it)) }
    }

    // Structured outputs reject open objects and optional properties.
    @Test
    fun everySchemaObjectIsClosedAndFullyRequired() {
        val objects = objectsIn(ITINERARY_SCHEMA)
        assertEquals(3, objects.size)
        objects.forEach {
            assertEquals(false, it["additionalProperties"])
            assertEquals((it["properties"] as Map<*, *>).keys.toList(), it["required"])
        }
    }

    @Test(expected = JSONException::class)
    fun missingFieldIsRejected() {
        parseItinerary("""{"title": "Rome", "summary": "", "estimatedBudget": "", "tips": []}""")
    }

    private fun assertMatches(schema: Map<*, *>, value: Any?, path: String = "$") {
        when (schema["type"]) {
            "object" -> {
                val properties = schema["properties"] as Map<*, *>
                assertEquals(path, properties.keys, (value as JSONObject).keys().asSequence().toSet())
                properties.forEach { (key, child) -> assertMatches(child as Map<*, *>, value.get(key as String), "$path.$key") }
            }
            "array" -> (value as JSONArray).let { array ->
                repeat(array.length()) { assertMatches(schema["items"] as Map<*, *>, array.get(it), "$path[$it]") }
            }
            "string" -> assertTrue(path, value is String)
            "integer" -> assertTrue(path, value is Int)
            else -> error("Unexpected schema type at $path")
        }
    }

    private fun objectsIn(schema: Map<*, *>): List<Map<*, *>> = when (schema["type"]) {
        "object" -> listOf(schema) + (schema["properties"] as Map<*, *>).values.flatMap { objectsIn(it as Map<*, *>) }
        "array" -> objectsIn(schema["items"] as Map<*, *>)
        else -> emptyList()
    }
}
