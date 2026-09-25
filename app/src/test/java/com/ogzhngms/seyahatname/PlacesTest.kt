package com.ogzhngms.seyahatname

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlacesTest {
    private fun assertAt(lat: Float, lon: Float, text: String) {
        val place = findPlace(text) ?: throw AssertionError("no place for $text")
        assertEquals(lat, place.lat, 0.01f)
        assertEquals(lon, place.lon, 0.01f)
    }

    @Test
    fun findsPlacesWhateverTheCaseAccentsOrExtraWords() {
        assertAt(41.01f, 28.98f, "İSTANBUL")
        assertAt(41.01f, 28.98f, "istanbul'a gidiyorum")
        assertAt(41.90f, 12.50f, "Roma, İtalya")
        assertAt(41.90f, 12.50f, "ローマ")
        assertAt(40.71f, -74.01f, "New York City")
        assertAt(37.16f, 38.79f, "sanliurfa")
        assertAt(-33.87f, 151.21f, "Sydney")
    }

    @Test
    fun unknownPlacesAndBlankTextFindNothing() {
        assertNull(findPlace("Atlantis"))
        assertNull(findPlace(""))
        assertNull(findPlace("   "))
    }
}
