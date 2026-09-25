package com.ogzhngms.seyahatname

import androidx.annotation.StringRes
import java.util.Locale

// Wizard order; TripViewModel and the question screen both follow this list.
val QUESTIONS = listOf(
    R.string.q_destination,
    R.string.q_days,
    R.string.q_companions,
    R.string.q_budget,
    R.string.q_interests,
    R.string.q_pace,
)

const val MAX_DAYS = 14

enum class Companions(@StringRes val label: Int, val prompt: String) {
    SOLO(R.string.companions_solo, "solo"),
    PARTNER(R.string.companions_partner, "as a couple"),
    FAMILY(R.string.companions_family, "with family"),
    FRIENDS(R.string.companions_friends, "with friends"),
}

enum class Budget(@StringRes val label: Int, val prompt: String) {
    LOW(R.string.budget_low, "low, keep costs down"),
    MEDIUM(R.string.budget_medium, "mid-range"),
    HIGH(R.string.budget_high, "luxury"),
}

enum class Interest(@StringRes val label: Int, val prompt: String) {
    CULTURE(R.string.interest_culture, "history and culture"),
    FOOD(R.string.interest_food, "food and drink"),
    NATURE(R.string.interest_nature, "nature"),
    ART(R.string.interest_art, "museums and art"),
    SHOPPING(R.string.interest_shopping, "shopping"),
    NIGHTLIFE(R.string.interest_nightlife, "nightlife"),
    BEACH(R.string.interest_beach, "beaches"),
    ADVENTURE(R.string.interest_adventure, "adventure"),
}

enum class Pace(@StringRes val label: Int, val prompt: String) {
    RELAXED(R.string.pace_relaxed, "relaxed, two or three activities a day"),
    BALANCED(R.string.pace_balanced, "balanced, four or five activities a day"),
    PACKED(R.string.pace_packed, "packed, six or more activities a day"),
}

data class TripAnswers(
    val destination: String = "",
    val days: Int = 3,
    val companions: Companions = Companions.SOLO,
    val budget: Budget = Budget.MEDIUM,
    val interests: Set<Interest> = emptySet(),
    val notes: String = "",
    val pace: Pace = Pace.BALANCED,
)

const val SYSTEM_PROMPT =
    "You are a travel planner. Turn the traveller's answers into a realistic day-by-day itinerary. " +
        "Keep each day in one area of the destination, give every activity a start time in 24-hour HH:MM format, " +
        "estimate costs per person in the currency asked for, and keep each description to one or two sentences."

fun buildPrompt(answers: TripAnswers, language: String, currency: Currency = Currency.TRY): String = buildString {
    appendLine("Destination: ${answers.destination.trim()}")
    appendLine("Length: exactly ${answers.days} days")
    appendLine("Travelling: ${answers.companions.prompt}")
    appendLine("Budget: ${answers.budget.prompt}")
    appendLine("Currency: ${currency.name}")
    appendLine("Interests: " + answers.interests.sorted().joinToString { it.prompt }.ifEmpty { "no preference" })
    appendLine("Pace: ${answers.pace.prompt}")
    if (answers.notes.isNotBlank()) appendLine("Other wishes: ${answers.notes.trim()}")
    append("Write every text value in $language.")
}

// The model answers in the app's language, e.g. "Turkish"; a phone language the app lacks falls back to English.
fun promptLanguage(): String {
    val locale = Locale.getDefault().takeIf { current -> Language.entries.any { it.tag == current.language } } ?: Locale.ENGLISH
    return locale.getDisplayLanguage(Locale.ENGLISH)
}
