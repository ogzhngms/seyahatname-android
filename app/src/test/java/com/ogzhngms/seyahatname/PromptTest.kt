package com.ogzhngms.seyahatname

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptTest {
    @Test
    fun promptCarriesEveryAnswerAndTheLanguage() {
        val answers = TripAnswers(
            destination = " Rome ",
            days = 4,
            companions = Companions.FRIENDS,
            budget = Budget.LOW,
            interests = setOf(Interest.NIGHTLIFE, Interest.FOOD),
            notes = "No museums",
            pace = Pace.PACKED,
        )
        assertEquals(
            """
            Destination: Rome
            Length: exactly 4 days
            Travelling: with friends
            Budget: low, keep costs down
            Currency: EUR
            Interests: food and drink, nightlife
            Pace: packed, six or more activities a day
            Other wishes: No museums
            Write every text value in Turkish.
            """.trimIndent(),
            buildPrompt(answers, "Turkish", Currency.EUR),
        )
    }

    @Test
    fun skippedAnswersStayOutOfThePrompt() {
        val prompt = buildPrompt(TripAnswers(destination = "Rome"), "English")
        assertTrue("Interests: no preference" in prompt)
        assertTrue("Currency: the local currency of the destination" in prompt)
        assertFalse("Other wishes" in prompt)
    }
}
