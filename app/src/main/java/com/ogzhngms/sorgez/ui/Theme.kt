package com.ogzhngms.sorgez.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Ogi's palette: night navy with Android green.
private val Green = Color(0xFF3DDC84)
private val Night = Color(0xFF0D1117)
private val Panel = Color(0xFF15212B)
private val Ink = Color(0xFFF0F6FC)
private val Mist = Color(0xFFA8B6C5)

private val colors = darkColorScheme(
    primary = Green,
    onPrimary = Night,
    secondaryContainer = Color(0xFF1C3B2B),
    onSecondaryContainer = Green,
    tertiary = Mist,
    background = Night,
    onBackground = Ink,
    surface = Night,
    onSurface = Ink,
    surfaceVariant = Panel,
    onSurfaceVariant = Mist,
    surfaceContainer = Panel,
    surfaceContainerHigh = Panel,
    surfaceContainerHighest = Panel,
    outline = Color(0xFF2D3B48),
    outlineVariant = Color(0xFF25313C),
)

@Composable
fun SorGezTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
