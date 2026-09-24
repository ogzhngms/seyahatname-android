package com.ogzhngms.seyahatname

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import java.util.Locale

enum class Language(val tag: String, val label: String) {
    ENGLISH("en", "English"),
    TURKISH("tr", "Türkçe"),
    SPANISH("es", "Español"),
    AZERBAIJANI("az", "Azərbaycan"),
}

enum class Planet(@StringRes val label: Int) {
    EARTH(R.string.planet_earth),
    MOON(R.string.planet_moon),
    SUN(R.string.planet_sun),
}

// The language and home-screen planet picked on the profile screen, kept on the device.
object AppSettings {
    private fun preferences(context: Context) = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun planet(context: Context): Planet {
        val name = preferences(context).getString("planet", null)
        return Planet.entries.firstOrNull { it.name == name } ?: Planet.EARTH
    }

    fun savePlanet(context: Context, planet: Planet) {
        preferences(context).edit().putString("planet", planet.name).apply()
    }

    fun saveLanguage(context: Context, language: Language) {
        preferences(context).edit().putString("language", language.tag).apply()
    }

    // Applies the picked language to a context; with no pick the phone's language is used.
    fun wrap(base: Context): Context {
        val tag = preferences(base).getString("language", null) ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        return base.createConfigurationContext(Configuration(base.resources.configuration).apply { setLocale(locale) })
    }
}
