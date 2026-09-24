package com.ogzhngms.seyahatname

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

// A missing translation does not fail the build; Android quietly shows the English text instead.
class TranslationsTest {
    private fun keys(folder: String): Set<String> =
        Regex("""<(?:string|plurals) name="([^"]+)"""")
            .findAll(File("src/main/res/$folder/strings.xml").readText())
            .map { it.groupValues[1] }
            .toSet()

    @Test
    fun everyLanguageInThePickerHasEveryString() {
        val english = keys("values") - "app_name"
        for (language in Language.entries - Language.ENGLISH) {
            assertEquals(language.label, english, keys("values-${language.tag}"))
        }
    }
}
