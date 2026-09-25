package com.ogzhngms.seyahatname

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ogzhngms.seyahatname.ui.SorGezApp
import com.ogzhngms.seyahatname.ui.SorGezTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val demo = BuildConfig.GEMINI_API_KEY.isBlank()

    private val viewModel: TripViewModel by viewModels {
        viewModelFactory { initializer { TripViewModel(planner()) } }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppSettings.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.signInAndUpload(applicationContext)
        // The app is always dark, so keep the system bar icons light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            SorGezTheme {
                SorGezApp(viewModel, demo, onLanguageChange = { AppSettings.saveLanguage(this, it); recreate() })
            }
        }
    }

    // Without an API key the app plays back a bundled sample plan, so the whole flow still works.
    private fun planner(): suspend (TripAnswers) -> String {
        val app = application
        if (demo) return {
            delay(1500)
            // Read at call time so the sample follows a language picked after launch.
            AppSettings.wrap(app).resources.openRawResource(R.raw.sample_itinerary).bufferedReader().use { it.readText() }
        }
        val gemini = GeminiPlanner(BuildConfig.GEMINI_API_KEY)
        return { answers -> gemini.plan(buildPrompt(answers, promptLanguage(), AppSettings.currency(app))) }
    }
}
