package com.ogzhngms.seyahatname.ui

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ogzhngms.seyahatname.BuildConfig
import com.ogzhngms.seyahatname.Currency
import com.ogzhngms.seyahatname.Language
import com.ogzhngms.seyahatname.Planet
import com.ogzhngms.seyahatname.R
import java.util.Locale

// The landing screen: the chosen planet in the middle with Start at its centre.
@Composable
internal fun HomeScreen(planet: Planet, onStart: () -> Unit, onProfile: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        RouteBackdrop(planet, Modifier.fillMaxSize())
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onProfile) {
                Icon(
                    painterResource(R.drawable.ic_account_circle),
                    contentDescription = stringResource(R.string.cd_profile),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Box(Modifier.align(Alignment.Center).fillMaxWidth(0.9f), contentAlignment = Alignment.Center) {
            PlanetView(planet, Modifier.fillMaxWidth())
            // A glass pill: the planet shows through, and a hairline edge keeps it readable on the bright Sun.
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.32f), contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
            ) {
                Text(
                    stringResource(R.string.action_start),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp,
                )
            }
        }
        Text(
            stringResource(R.string.home_tagline),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 24.dp, end = 24.dp, bottom = 48.dp),
        )
    }
}

// Where a flight leaves from and lands, as shares of the screen, and how far its arc bows.
private class Route(val from: Offset, val to: Offset, val bend: Float)

// Laid around the planet so most arcs sit in the empty space above and below it; one passes behind it.
private val ROUTES = listOf(
    Route(Offset(0.08f, 0.20f), Offset(0.62f, 0.12f), -0.25f),
    Route(Offset(0.55f, 0.25f), Offset(0.94f, 0.34f), -0.35f),
    Route(Offset(0.04f, 0.40f), Offset(0.95f, 0.60f), 0.30f),
    Route(Offset(0.10f, 0.79f), Offset(0.58f, 0.87f), 0.30f),
    Route(Offset(0.42f, 0.72f), Offset(0.93f, 0.80f), -0.30f),
)

// Dashed flight arcs with a plane dot moving along each, tinted to match the planet.
@Composable
private fun RouteBackdrop(planet: Planet, modifier: Modifier) {
    val tint by animateColorAsState(
        when (planet) {
            Planet.EARTH -> MaterialTheme.colorScheme.primary
            Planet.MOON -> Color(0xFFB8C4D6)
            Planet.SUN -> Color(0xFFFFB347)
        },
        label = "tint",
    )
    val progress by rememberInfiniteTransition(label = "flights").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9_000, easing = LinearEasing)),
        label = "flights",
    )
    Canvas(modifier) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()), -progress * 24.dp.toPx())
        ROUTES.forEachIndexed { index, route ->
            val from = Offset(route.from.x * size.width, route.from.y * size.height)
            val to = Offset(route.to.x * size.width, route.to.y * size.height)
            val middle = (from + to) / 2f
            val control = middle + Offset(-(to - from).y, (to - from).x) * route.bend
            val path = Path().apply {
                moveTo(from.x, from.y)
                quadraticTo(control.x, control.y, to.x, to.y)
            }
            drawPath(path, tint.copy(alpha = 0.28f), style = Stroke(1.5.dp.toPx(), pathEffect = dash))
            listOf(from, to).forEach {
                drawCircle(tint.copy(alpha = 0.5f), 4.dp.toPx(), it, style = Stroke(1.5.dp.toPx()))
                drawCircle(tint.copy(alpha = 0.5f), 1.5.dp.toPx(), it)
            }
            // Flights start at different times so the dots never move in step.
            val measure = PathMeasure().apply { setPath(path, false) }
            val plane = measure.getPosition((progress + index * 0.37f) % 1f * measure.length)
            drawCircle(tint.copy(alpha = 0.25f), 7.dp.toPx(), plane)
            drawCircle(tint, 3.dp.toPx(), plane)
        }
    }
}

@Composable
internal fun ProfileScreen(
    planet: Planet,
    language: Language?,
    currency: Currency,
    onPlanet: (Planet) -> Unit,
    onLanguage: (Language) -> Unit,
    onCurrency: (Currency) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val openLink = LocalUriHandler.current
    // Sign-in is a placeholder for now.
    val comingSoon = { Toast.makeText(context, R.string.coming_soon, Toast.LENGTH_SHORT).show() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.cd_back))
            }
            Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_account_circle),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(72.dp),
                )
                Text(stringResource(R.string.account_guest), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.account_body), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Button(onClick = comingSoon, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sign_in_google)) }
                OutlinedButton(onClick = comingSoon, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sign_in_email)) }
            }
        }
        Section(R.string.theme_title) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Planet.entries.forEach { option ->
                    PlanetOption(option, selected = option == planet, onClick = { onPlanet(option) }, modifier = Modifier.weight(1f))
                }
            }
        }
        Section(R.string.language_title) {
            Picker(
                R.string.language_title,
                Language.entries,
                language,
                mark = { "${it.flag}  ${it.code}" },
                name = { it.label },
                onPick = onLanguage,
            )
        }
        Section(R.string.currency_title) {
            Text(stringResource(R.string.currency_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
            // Currency names come from the platform, already in the app's language.
            Picker(
                R.string.currency_title,
                Currency.entries,
                currency,
                mark = { "${it.symbol}  ${it.name}" },
                name = { java.util.Currency.getInstance(it.name).getDisplayName(Locale.getDefault()) },
                onPick = onCurrency,
            )
        }
        Section(R.string.about_title) {
            Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.about_images), color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { openLink.openUri(SOURCE_URL) }, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(R.string.about_source))
            }
        }
    }
}

private const val SOURCE_URL = "https://github.com/ogzhngms/seyahatname-android"

// One button showing the current choice; it opens the full list with a short mark and a name for each option.
@Composable
private fun <T> Picker(
    @StringRes title: Int,
    options: List<T>,
    current: T?,
    mark: @Composable (T) -> String,
    name: @Composable (T) -> String,
    button: @Composable (T) -> String = mark,
    onPick: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val description = "${stringResource(title)}: ${current?.let { name(it) }.orEmpty()}"
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.semantics { contentDescription = description }) {
            Text(current?.let { button(it) } ?: "🌐", style = MaterialTheme.typography.titleMedium)
            Icon(painterResource(R.drawable.ic_expand_more), contentDescription = null, modifier = Modifier.padding(start = 8.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                val selected = option == current
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                mark(option),
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(72.dp),
                            )
                            Text(name(option), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    trailingIcon = {
                        if (selected) Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    onClick = {
                        open = false
                        if (!selected) onPick(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun Section(@StringRes title: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun PlanetOption(planet: Planet, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier.selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PlanetView(planet, Modifier.size(64.dp))
            Text(
                stringResource(planet.label),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
