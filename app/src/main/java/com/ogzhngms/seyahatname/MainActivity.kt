package com.ogzhngms.seyahatname

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ogzhngms.seyahatname.ui.SeyahatnameApp
import com.ogzhngms.seyahatname.ui.SeyahatnameTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val demo = BuildConfig.GEMINI_API_KEY.isBlank()

    private val viewModel: TripViewModel by viewModels {
        viewModelFactory { initializer { TripViewModel(planner()) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark, so keep the system bar icons light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent { SeyahatnameTheme { SeyahatnameApp(viewModel, demo) } }
    }

    // Without an API key the app plays back a bundled sample plan, so the whole flow still works.
    private fun planner(): suspend (TripAnswers) -> PlanReply {
        val resources = application.resources
        if (demo) return {
            delay(1500)
            PlanReply(resources.openRawResource(R.raw.sample_itinerary).bufferedReader().use { it.readText() }, "Demo")
        }
        val gemini = GeminiPlanner(BuildConfig.GEMINI_API_KEY)
        return { answers -> gemini.plan(buildPrompt(answers, promptLanguage()), answers.model) }
    }
}
