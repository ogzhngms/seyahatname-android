package com.ogzhngms.seyahatname

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ogzhngms.seyahatname.ui.SeyahatnameApp
import com.ogzhngms.seyahatname.ui.SeyahatnameTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Drives the real screens with a stub planner, so no API key or network is needed.
@RunWith(AndroidJUnit4::class)
class TripFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val sample = context.resources.openRawResource(R.raw.sample_itinerary).bufferedReader().use { it.readText() }

    private fun text(id: Int) = context.getString(id)
    private fun next() = compose.onNodeWithText(text(R.string.action_next)).performClick()

    @Test
    fun answersBecomeAPlanOnScreen() {
        var sent: TripAnswers? = null
        val vm = TripViewModel { sent = it; sample }
        compose.setContent { SeyahatnameTheme { SeyahatnameApp(vm, demo = false) } }

        compose.onNodeWithText(text(R.string.action_next)).assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("Rome")
        next()
        compose.onNodeWithContentDescription(text(R.string.cd_more_days)).performClick()
        next()
        compose.onNodeWithText(text(R.string.companions_friends)).performClick()
        next()
        next()
        compose.onNodeWithText(text(R.string.interest_food)).performClick()
        next()
        next()

        compose.onNodeWithText("Rome").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.action_plan)).performClick()

        compose.onNodeWithText(parseItinerary(sample).title).assertIsDisplayed()
        assertEquals(
            TripAnswers(destination = "Rome", days = 4, companions = Companions.FRIENDS, interests = setOf(Interest.FOOD)),
            sent,
        )
    }

    @Test
    fun failedPlanCanBeRetried() {
        var online = false
        val vm = TripViewModel { if (online) sample else error("offline") }
        compose.setContent { SeyahatnameTheme { SeyahatnameApp(vm, demo = false) } }
        vm.update { it.copy(destination = "Rome") }
        repeat(QUESTIONS.size) { vm.next() }

        compose.onNodeWithText(text(R.string.action_plan)).performClick()
        compose.onNodeWithText(text(R.string.error_generic)).assertIsDisplayed()

        online = true
        compose.onNodeWithText(text(R.string.action_retry)).performClick()
        compose.onNodeWithText(parseItinerary(sample).title).assertIsDisplayed()
    }

    // With the keyboard open, the first Back only closes it; the second one leaves the step.
    @Test
    fun backClosesTheKeyboardBeforeLeavingTheStep() {
        val vm = TripViewModel { sample }
        compose.setContent { SeyahatnameTheme { SeyahatnameApp(vm, demo = false) } }
        vm.update { it.copy(destination = "Rome") }
        repeat(4) { vm.next() }

        compose.onNodeWithText(text(R.string.hint_notes)).performClick()
        compose.waitUntil(5_000) { keyboardShown() }

        compose.onNodeWithText(text(R.string.action_back)).performClick()
        compose.waitUntil(5_000) { !keyboardShown() }
        assertEquals(Screen.Question(4), vm.screen)

        compose.onNodeWithText(text(R.string.action_back)).performClick()
        compose.waitForIdle()
        assertEquals(Screen.Question(3), vm.screen)
    }

    private fun keyboardShown() =
        ViewCompat.getRootWindowInsets(compose.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == true
}
