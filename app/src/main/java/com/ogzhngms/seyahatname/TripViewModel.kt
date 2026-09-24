package com.ogzhngms.seyahatname

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface Screen {
    data class Question(val step: Int) : Screen
    data object Confirm : Screen
    data object Loading : Screen
    data class Result(val itinerary: Itinerary, val json: String) : Screen
    data class Failed(@StringRes val message: Int, val detail: String?) : Screen
}

// planner turns the answers into the itinerary JSON: Claude in the app, a stub in tests.
class TripViewModel(private val planner: suspend (TripAnswers) -> String) : ViewModel() {
    var answers by mutableStateOf(TripAnswers())
        private set
    var screen by mutableStateOf<Screen>(Screen.Question(0))
        private set
    private var job: Job? = null

    fun update(change: (TripAnswers) -> TripAnswers) {
        answers = change(answers)
    }

    fun next() {
        val step = (screen as? Screen.Question)?.step ?: return
        if (step == 0 && answers.destination.isBlank()) return
        screen = if (step < QUESTIONS.lastIndex) Screen.Question(step + 1) else Screen.Confirm
    }

    fun back() {
        screen = when (val current = screen) {
            is Screen.Question -> Screen.Question((current.step - 1).coerceAtLeast(0))
            Screen.Confirm -> Screen.Question(QUESTIONS.lastIndex)
            else -> {
                job?.cancel()
                Screen.Confirm
            }
        }
    }

    fun submit() {
        job?.cancel()
        screen = Screen.Loading
        job = viewModelScope.launch {
            screen = try {
                val json = planner(answers)
                Screen.Result(parseItinerary(json), json)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Screen.Failed(errorMessage(e), errorDetail(e))
            }
        }
    }

    fun restart() {
        answers = TripAnswers()
        screen = Screen.Question(0)
    }
}
