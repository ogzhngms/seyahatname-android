package com.ogzhngms.seyahatname

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.annotation.StringRes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Locale

// A language, the flag shown next to it in the picker, and its name in that language.
// Android's own resource code for Indonesian is "in"; the picker shows the usual "ID".
enum class Language(val tag: String, val flag: String, val label: String) {
    ENGLISH("en", "🇬🇧", "English"),
    TURKISH("tr", "🇹🇷", "Türkçe"),
    SPANISH("es", "🇪🇸", "Español"),
    GERMAN("de", "🇩🇪", "Deutsch"),
    FRENCH("fr", "🇫🇷", "Français"),
    ITALIAN("it", "🇮🇹", "Italiano"),
    PORTUGUESE("pt", "🇧🇷", "Português"),
    RUSSIAN("ru", "🇷🇺", "Русский"),
    ARABIC("ar", "🇸🇦", "العربية"),
    HINDI("hi", "🇮🇳", "हिन्दी"),
    CHINESE("zh", "🇨🇳", "中文"),
    JAPANESE("ja", "🇯🇵", "日本語"),
    KOREAN("ko", "🇰🇷", "한국어"),
    INDONESIAN("in", "🇮🇩", "Bahasa Indonesia"),
    AZERBAIJANI("az", "🇦🇿", "Azərbaycan"),
    ;

    val code: String get() = if (this == INDONESIAN) "ID" else tag.uppercase()
}

enum class Planet(@StringRes val label: Int) {
    EARTH(R.string.planet_earth),
    MOON(R.string.planet_moon),
    SUN(R.string.planet_sun),
}

// The language and home-screen planet picked on the profile screen. The device copy is the one the app reads,
// since the language is needed before the first screen is drawn; a copy goes to Firestore under users/{uid}.
object AppSettings {
    private fun preferences(context: Context) = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun planet(context: Context): Planet {
        val name = preferences(context).getString("planet", null)
        return Planet.entries.firstOrNull { it.name == name } ?: Planet.EARTH
    }

    fun savePlanet(context: Context, planet: Planet) {
        preferences(context).edit().putString("planet", planet.name).apply()
        upload(context)
    }

    fun saveLanguage(context: Context, language: Language) {
        preferences(context).edit().putString("language", language.tag).apply()
        upload(context)
    }

    // There is no real sign-in yet, so each install gets an anonymous account;
    // later it can be linked to Google and keep the same uid and settings.
    fun signInAndUpload(context: Context) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) return upload(context)
        auth.signInAnonymously()
            .addOnSuccessListener { upload(context) }
            .addOnFailureListener { Log.w(TAG, "Anonymous sign-in failed", it) }
    }

    private fun upload(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val data = mutableMapOf<String, Any>("planet" to planet(context).name, "updatedAt" to FieldValue.serverTimestamp())
        preferences(context).getString("language", null)?.let { data["language"] = it }
        FirebaseFirestore.getInstance().collection("users").document(user.uid).set(data, SetOptions.merge())
            .addOnSuccessListener { Log.i(TAG, "Settings saved to Firestore for ${user.uid}") }
            .addOnFailureListener { Log.w(TAG, "Settings not saved to Firestore", it) }
    }

    // Applies the picked language to a context; with no pick the phone's language is used.
    fun wrap(base: Context): Context {
        val tag = preferences(base).getString("language", null) ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        return base.createConfigurationContext(Configuration(base.resources.configuration).apply { setLocale(locale) })
    }

    private const val TAG = "AppSettings"
}
