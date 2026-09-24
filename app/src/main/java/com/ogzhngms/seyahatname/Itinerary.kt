package com.ogzhngms.seyahatname

import org.json.JSONArray
import org.json.JSONObject

data class Itinerary(
    val title: String,
    val summary: String,
    val estimatedBudget: String,
    val days: List<Day>,
    val tips: List<String>,
)

data class Day(val day: Int, val title: String, val activities: List<Activity>)

data class Activity(val time: String, val title: String, val description: String, val cost: String)

private val STRING: Map<String, Any> = mapOf("type" to "string")

// The JSON shape the model is held to; parseItinerary() reads exactly these fields.
val ITINERARY_SCHEMA: Map<String, Any> = obj(
    "title" to STRING,
    "summary" to STRING,
    "estimatedBudget" to STRING,
    "days" to array(
        obj(
            "day" to mapOf("type" to "integer"),
            "title" to STRING,
            "activities" to array(
                obj("time" to STRING, "title" to STRING, "description" to STRING, "cost" to STRING),
            ),
        ),
    ),
    "tips" to array(STRING),
)

fun parseItinerary(json: String): Itinerary {
    val root = JSONObject(json)
    return Itinerary(
        title = root.getString("title"),
        summary = root.getString("summary"),
        estimatedBudget = root.getString("estimatedBudget"),
        days = root.getJSONArray("days").objects().map { day ->
            Day(
                day = day.getInt("day"),
                title = day.getString("title"),
                activities = day.getJSONArray("activities").objects().map {
                    Activity(it.getString("time"), it.getString("title"), it.getString("description"), it.getString("cost"))
                },
            )
        },
        tips = root.getJSONArray("tips").let { tips -> List(tips.length()) { tips.getString(it) } },
    )
}

private fun JSONArray.objects() = List(length()) { getJSONObject(it) }

// Structured outputs require every property to be listed as required and no extra properties.
private fun obj(vararg properties: Pair<String, Any>): Map<String, Any> = mapOf(
    "type" to "object",
    "properties" to mapOf(*properties),
    "required" to properties.map { it.first },
    "additionalProperties" to false,
)

private fun array(items: Any): Map<String, Any> = mapOf("type" to "array", "items" to items)
