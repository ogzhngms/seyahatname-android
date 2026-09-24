package com.ogzhngms.seyahatname.ui

import android.widget.Toast
import androidx.annotation.StringRes
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ogzhngms.seyahatname.Language
import com.ogzhngms.seyahatname.Planet
import com.ogzhngms.seyahatname.R
import kotlin.random.Random

// The landing screen: the chosen planet in the middle with Start at its centre.
@Composable
internal fun HomeScreen(planet: Planet, onStart: () -> Unit, onProfile: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Stars(Modifier.fillMaxSize())
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
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 36.dp, vertical = 16.dp),
            ) { Text(stringResource(R.string.action_start), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        }
        Text(
            stringResource(R.string.home_tagline),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 24.dp, end = 24.dp, bottom = 48.dp),
        )
    }
}

@Composable
private fun Stars(modifier: Modifier) {
    val stars = remember { Random(7).let { random -> List(80) { Offset(random.nextFloat(), random.nextFloat()) to random.nextFloat() } } }
    Canvas(modifier) {
        stars.forEach { (at, brightness) ->
            drawCircle(
                Color.White.copy(alpha = 0.15f + 0.5f * brightness),
                radius = 1f + 1.5f * brightness,
                center = Offset(at.x * size.width, at.y * size.height),
            )
        }
    }
}

@Composable
internal fun ProfileScreen(
    planet: Planet,
    language: Language?,
    onPlanet: (Planet) -> Unit,
    onLanguage: (Language) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
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
            Choices(Language.entries, { it == language }, { it.label }, onLanguage)
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
