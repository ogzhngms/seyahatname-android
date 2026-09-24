package com.ogzhngms.seyahatname

import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripViewModelTest {
    private val sample = File("src/main/res/raw/sample_itinerary.json").readText()
    private val reply = PlanReply(sample, "Gemini 3.8 Flash")

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun firstStepNeedsADestination() {
        val vm = TripViewModel { reply }
        vm.next()
        vm.update { it.copy(destination = "   ") }
        vm.next()
        assertEquals(Screen.Question(0), vm.screen)
    }

    @Test
    fun answersReachThePlannerAndThePlanIsShown() {
        var sent: TripAnswers? = null
        val vm = TripViewModel { sent = it; reply }
        vm.update { it.copy(destination = "Rome", interests = setOf(Interest.FOOD)) }
        repeat(QUESTIONS.size) { vm.next() }
        assertEquals(Screen.Confirm, vm.screen)

        vm.submit()

        assertEquals(vm.answers, sent)
        val result = vm.screen as Screen.Result
        assertEquals(sample, result.json)
        assertEquals("Gemini 3.8 Flash", result.model)
        assertEquals(3, result.itinerary.days.size)
    }

    @Test
    fun plannerFailureShowsAnErrorAndRetryRecovers() {
        var online = false
        val vm = TripViewModel { if (online) reply else throw IOException("offline") }
        vm.submit()
        assertEquals(Screen.Failed(R.string.error_network, null), vm.screen)

        online = true
        vm.submit()
        assertTrue(vm.screen is Screen.Result)
    }

    @Test
    fun replyThatBreaksTheSchemaShowsAParseError() {
        val vm = TripViewModel { PlanReply("""{"title": "Rome"}""", "Gemini 3.8 Flash") }
        vm.submit()
        assertEquals(Screen.Failed(R.string.error_parse, null), vm.screen)
    }

    @Test
    fun backWhileLoadingCancelsAndIgnoresTheLateReply() {
        val late = CompletableDeferred<PlanReply>()
        val vm = TripViewModel { late.await() }
        vm.submit()
        assertEquals(Screen.Loading, vm.screen)

        vm.back()
        late.complete(reply)

        assertEquals(Screen.Confirm, vm.screen)
    }
}
