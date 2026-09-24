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

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun firstStepNeedsADestination() {
        val vm = TripViewModel { sample }
        vm.next()
        vm.update { it.copy(destination = "   ") }
        vm.next()
        assertEquals(Screen.Question(0), vm.screen)
    }

    @Test
    fun answersReachThePlannerAndThePlanIsShown() {
        var sent: TripAnswers? = null
        val vm = TripViewModel { sent = it; sample }
        vm.update { it.copy(destination = "Rome", interests = setOf(Interest.FOOD)) }
        repeat(QUESTIONS.size) { vm.next() }
        assertEquals(Screen.Confirm, vm.screen)

        vm.submit()

        assertEquals(vm.answers, sent)
        val result = vm.screen as Screen.Result
        assertEquals(sample, result.json)
        assertEquals(3, result.itinerary.days.size)
    }

    @Test
    fun plannerFailureShowsAnErrorAndRetryRecovers() {
        var online = false
        val vm = TripViewModel { if (online) sample else throw IOException("offline") }
        vm.submit()
        assertEquals(Screen.Failed(R.string.error_network, null), vm.screen)

        online = true
        vm.submit()
        assertTrue(vm.screen is Screen.Result)
    }

    @Test
    fun replyThatBreaksTheSchemaShowsAParseError() {
        val vm = TripViewModel { """{"title": "Rome"}""" }
        vm.submit()
        assertEquals(Screen.Failed(R.string.error_parse, null), vm.screen)
    }

    @Test
    fun backWhileLoadingCancelsAndIgnoresTheLateReply() {
        val reply = CompletableDeferred<String>()
        val vm = TripViewModel { reply.await() }
        vm.submit()
        assertEquals(Screen.Loading, vm.screen)

        vm.back()
        reply.complete(sample)

        assertEquals(Screen.Confirm, vm.screen)
    }
}
