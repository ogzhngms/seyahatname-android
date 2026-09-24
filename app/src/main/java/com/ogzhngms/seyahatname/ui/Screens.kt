package com.ogzhngms.seyahatname.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ogzhngms.seyahatname.AppSettings
import com.ogzhngms.seyahatname.Budget
import com.ogzhngms.seyahatname.Companions
import com.ogzhngms.seyahatname.Day
import com.ogzhngms.seyahatname.Interest
import com.ogzhngms.seyahatname.Itinerary
import com.ogzhngms.seyahatname.Currency
import com.ogzhngms.seyahatname.Language
import com.ogzhngms.seyahatname.MAX_DAYS
import com.ogzhngms.seyahatname.Pace
import com.ogzhngms.seyahatname.QUESTIONS
import com.ogzhngms.seyahatname.R
import com.ogzhngms.seyahatname.Screen
import com.ogzhngms.seyahatname.TripAnswers
import com.ogzhngms.seyahatname.TripViewModel
import com.ogzhngms.seyahatname.buildPrompt
import com.ogzhngms.seyahatname.promptLanguage
import java.util.Locale
import org.json.JSONObject

@Composable
fun SeyahatnameApp(vm: TripViewModel, demo: Boolean, onLanguageChange: (Language) -> Unit = {}) {
    val context = LocalContext.current
    var planet by remember { mutableStateOf(AppSettings.planet(context)) }
    var currency by remember { mutableStateOf(AppSettings.currency(context)) }
    val language = Language.entries.firstOrNull { it.tag == Locale.getDefault().language }
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    // Back closes an open keyboard first; only the next press leaves the step.
    // The keyboard is checked at press time, so a second press during its closing animation still goes back.
    val back = {
        val keyboardOpen = ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.ime()) == true
        if (keyboardOpen) focusManager.clearFocus() else vm.back()
    }
    BackHandler(enabled = vm.screen != Screen.Home, onBack = back)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedContent(vm.screen, transitionSpec = { transition(initialState, targetState) }, label = "screen") { screen ->
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                when (screen) {
                    Screen.Home -> HomeScreen(planet, onStart = vm::start, onProfile = vm::openProfile)
                    Screen.Profile -> ProfileScreen(
                        planet,
                        language,
                        currency,
                        onPlanet = { planet = it; AppSettings.savePlanet(context, it) },
                        onLanguage = onLanguageChange,
                        onCurrency = { currency = it; AppSettings.saveCurrency(context, it) },
                        onBack = vm::back,
                    )
                    is Screen.Question -> QuestionScreen(screen.step, vm.answers, vm::update, vm::next, back)
                    Screen.Confirm -> ConfirmScreen(vm.answers, currency, demo, onPlan = vm::submit, onEdit = vm::back)
                    Screen.Loading -> LoadingScreen(onCancel = vm::back)
                    is Screen.Result -> ResultScreen(screen, demo, onNewPlan = vm::restart)
                    is Screen.Failed -> FailedScreen(screen, onRetry = vm::submit, onEdit = vm::back)
                }
            }
        }
    }
}

// Start zooms into the planet, question steps slide sideways, everything else cross-fades.
private fun AnimatedContentTransitionScope<Screen>.transition(from: Screen, to: Screen): ContentTransform = when {
    from == Screen.Home && to is Screen.Question ->
        (fadeIn(tween(400, delayMillis = 250)) + scaleIn(tween(400, delayMillis = 250), initialScale = 0.9f)) togetherWith
            (fadeOut(tween(450)) + scaleOut(tween(550), targetScale = 2.5f))
    from is Screen.Question && to == Screen.Home ->
        (fadeIn(tween(450)) + scaleIn(tween(550), initialScale = 2.5f)) togetherWith
            (fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.9f))
    from is Screen.Question && to is Screen.Question -> {
        val forward = to.step > from.step
        (slideInHorizontally { if (forward) it else -it } + fadeIn()) togetherWith
            (slideOutHorizontally { if (forward) -it else it } + fadeOut())
    }
    else -> fadeIn(tween(300)) togetherWith fadeOut(tween(300))
}

@Composable
private fun QuestionScreen(
    step: Int,
    answers: TripAnswers,
    onUpdate: ((TripAnswers) -> TripAnswers) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        LinearProgressIndicator(progress = { (step + 1f) / QUESTIONS.size }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.step_counter, step + 1, QUESTIONS.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(QUESTIONS[step]), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (step) {
                0 -> OutlinedTextField(
                    value = answers.destination,
                    onValueChange = { value -> onUpdate { it.copy(destination = value) } },
                    placeholder = { Text(stringResource(R.string.hint_destination)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { onNext() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                1 -> DayPicker(answers.days) { days -> onUpdate { it.copy(days = days) } }
                2 -> Choices(Companions.entries, { it == answers.companions }, { stringResource(it.label) }) { choice ->
                    onUpdate { it.copy(companions = choice) }
                }
                3 -> Choices(Budget.entries, { it == answers.budget }, { stringResource(it.label) }) { choice ->
                    onUpdate { it.copy(budget = choice) }
                }
                4 -> {
                    Choices(Interest.entries, { it in answers.interests }, { stringResource(it.label) }) { choice ->
                        onUpdate { it.copy(interests = if (choice in it.interests) it.interests - choice else it.interests + choice) }
                    }
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(
                        value = answers.notes,
                        onValueChange = { value -> onUpdate { it.copy(notes = value) } },
                        label = { Text(stringResource(R.string.hint_notes)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                5 -> Choices(Pace.entries, { it == answers.pace }, { stringResource(it.label) }) { choice ->
                    onUpdate { it.copy(pace = choice) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_back)) }
            Button(
                onClick = onNext,
                enabled = step != 0 || answers.destination.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.action_next)) }
        }
    }
}

@Composable
private fun DayPicker(days: Int, onChange: (Int) -> Unit) {
    val fewer = stringResource(R.string.cd_fewer_days)
    val more = stringResource(R.string.cd_more_days)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        FilledTonalButton(
            onClick = { onChange(days - 1) },
            enabled = days > 1,
            modifier = Modifier.semantics { contentDescription = fewer },
        ) { Text("−", style = MaterialTheme.typography.titleLarge) }
        Text(pluralStringResource(R.plurals.days, days, days), style = MaterialTheme.typography.headlineSmall)
        FilledTonalButton(
            onClick = { onChange(days + 1) },
            enabled = days < MAX_DAYS,
            modifier = Modifier.semantics { contentDescription = more },
        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}

@Composable
private fun <T> Choices(options: List<T>, selected: (T) -> Boolean, label: @Composable (T) -> String, onClick: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = selected(option),
                onClick = { onClick(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

@Composable
private fun ConfirmScreen(answers: TripAnswers, currency: Currency, demo: Boolean, onPlan: () -> Unit, onEdit: () -> Unit) {
    var showPrompt by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.confirm_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Summary(R.string.label_destination, answers.destination.trim())
                    Summary(R.string.label_days, pluralStringResource(R.plurals.days, answers.days, answers.days))
                    Summary(R.string.label_companions, stringResource(answers.companions.label))
                    Summary(R.string.label_budget, stringResource(answers.budget.label))
                    Summary(
                        R.string.label_interests,
                        answers.interests.sorted().map { stringResource(it.label) }.joinToString()
                            .ifEmpty { stringResource(R.string.interests_none) },
                    )
                    if (answers.notes.isNotBlank()) Summary(R.string.label_notes, answers.notes.trim())
                    Summary(R.string.label_pace, stringResource(answers.pace.label))
                }
            }
            Text(stringResource(R.string.confirm_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (demo) DemoBanner()
            TextButton(onClick = { showPrompt = !showPrompt }) {
                Text(stringResource(if (showPrompt) R.string.hide_prompt else R.string.show_prompt))
            }
            if (showPrompt) Code(buildPrompt(answers, promptLanguage(), currency))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_edit)) }
            Button(onClick = onPlan, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_plan)) }
        }
    }
}

@Composable
private fun Summary(@StringRes label: Int, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(label), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(110.dp))
        Text(value, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LoadingScreen(onCancel: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.loading_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.loading_body),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
    }
}

@Composable
private fun ResultScreen(result: Screen.Result, demo: Boolean, onNewPlan: () -> Unit) {
    val itinerary = result.itinerary
    var showJson by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (demo) item { DemoBanner() }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(itinerary.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(itinerary.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.budget_estimate, itinerary.estimatedBudget),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        items(itinerary.days) { day -> DayCard(day) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.tips_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    itinerary.tips.forEach { Text("• $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        item {
            Column {
                TextButton(onClick = { showJson = !showJson }) {
                    Text(stringResource(if (showJson) R.string.hide_json else R.string.show_json))
                }
                if (showJson) Code(remember(result.json) { JSONObject(result.json).toString(2) })
            }
        }
        item {
            Button(onClick = onNewPlan, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_new_plan)) }
        }
    }
}

@Composable
private fun DayCard(day: Day) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                stringResource(R.string.day_title, day.day, day.title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            day.activities.forEach { activity ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        activity.time,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(48.dp),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(activity.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(activity.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(activity.cost, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun FailedScreen(failed: Screen.Failed, onRetry: () -> Unit, onEdit: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.error_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(failed.message))
        failed.detail?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 6)
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_retry)) }
        OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_edit)) }
    }
}

@Composable
private fun DemoBanner() {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
        Text(
            stringResource(R.string.demo_banner),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun Code(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
        Text(text, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
    }
}
