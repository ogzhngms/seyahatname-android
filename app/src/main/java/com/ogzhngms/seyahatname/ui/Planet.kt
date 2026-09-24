package com.ogzhngms.seyahatname.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import com.ogzhngms.seyahatname.Planet
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// A mark on the sphere: longitude and latitude in degrees, size as a share of the radius.
private class Spot(val lon: Float, val lat: Float, val size: Float)

// Rough continents built from overlapping spots; enough to read as Earth while it turns.
private val LAND = listOf(
    Spot(-100f, 45f, 0.20f), Spot(-85f, 35f, 0.14f), Spot(-115f, 57f, 0.16f), Spot(-92f, 62f, 0.14f), Spot(-75f, 50f, 0.10f), Spot(-102f, 24f, 0.08f),
    Spot(-60f, -8f, 0.16f), Spot(-64f, -28f, 0.11f), Spot(-70f, -45f, 0.07f), Spot(-48f, -2f, 0.10f),
    Spot(12f, 50f, 0.10f), Spot(0f, 44f, 0.07f), Spot(20f, 8f, 0.18f), Spot(25f, -16f, 0.14f), Spot(8f, 24f, 0.14f), Spot(32f, 30f, 0.08f),
    Spot(80f, 50f, 0.22f), Spot(102f, 36f, 0.18f), Spot(60f, 62f, 0.16f), Spot(122f, 56f, 0.14f), Spot(78f, 22f, 0.10f), Spot(104f, 14f, 0.08f),
    Spot(134f, -25f, 0.13f),
)
private val CRATERS = listOf(
    Spot(-30f, 20f, 0.18f), Spot(40f, -25f, 0.14f), Spot(110f, 30f, 0.12f), Spot(-120f, -30f, 0.16f),
    Spot(170f, 5f, 0.10f), Spot(-70f, -50f, 0.09f), Spot(70f, 45f, 0.08f), Spot(0f, -60f, 0.07f),
)
private val SUNSPOTS = listOf(Spot(-40f, 15f, 0.12f), Spot(60f, -20f, 0.10f), Spot(150f, 30f, 0.14f), Spot(-140f, -35f, 0.09f))

// The home-screen planet, turning once every 40 seconds.
@Composable
fun PlanetView(planet: Planet, modifier: Modifier = Modifier) {
    val turn by rememberInfiniteTransition(label = "turn").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing)),
        label = "turn",
    )
    Canvas(modifier.aspectRatio(1f)) {
        val radius = size.minDimension / 2 * 0.8f
        when (planet) {
            Planet.EARTH -> {
                glow(radius, Color(0x664FA3FF))
                sphere(radius, listOf(Color(0xFF4FA3FF), Color(0xFF0D3B8C))) { spots(LAND, radius, turn, Color(0xFF3DDC84)) }
                shade(radius)
            }
            Planet.MOON -> {
                glow(radius, Color(0x33FFFFFF))
                sphere(radius, listOf(Color(0xFFEDEDED), Color(0xFF8E8E96))) { spots(CRATERS, radius, turn, Color(0x40000000)) }
                shade(radius)
            }
            Planet.SUN -> {
                // The sun is the light source: no shading, a pulsing corona instead.
                val pulse = 1f + 0.05f * sin(Math.toRadians(turn * 15.0)).toFloat()
                glow(radius * pulse, Color(0x99FFB300))
                sphere(radius, listOf(Color(0xFFFFF6C2), Color(0xFFFFC53D), Color(0xFFFF8A00))) {
                    spots(SUNSPOTS, radius, turn, Color(0x40FF6D00))
                }
            }
        }
    }
}

private fun DrawScope.glow(radius: Float, color: Color) {
    val outer = radius * 1.25f
    drawCircle(Brush.radialGradient(0.78f to color, 1f to Color.Transparent, center = center, radius = outer), outer)
}

// Lit from the top left.
private fun DrawScope.sphere(radius: Float, colors: List<Color>, surface: DrawScope.() -> Unit) {
    drawCircle(Brush.radialGradient(colors, center - Offset(radius * 0.35f, radius * 0.35f), radius * 1.5f), radius)
    clipPath(Path().apply { addOval(Rect(center, radius)) }) { surface() }
}

private fun DrawScope.shade(radius: Float) {
    drawCircle(
        Brush.radialGradient(0.55f to Color.Transparent, 1f to Color(0xAA000000), center = center - Offset(radius * 0.3f, radius * 0.3f), radius = radius * 1.6f),
        radius,
    )
}

// Orthographic projection of the turning sphere: spots on the far side are skipped,
// and spots near the edge get narrower and fainter.
private fun DrawScope.spots(spots: List<Spot>, radius: Float, turn: Float, color: Color) {
    for (spot in spots) {
        val lon = Math.toRadians((spot.lon + turn).toDouble())
        val lat = Math.toRadians(spot.lat.toDouble())
        val facing = (cos(lat) * cos(lon)).toFloat()
        if (facing <= 0f) continue
        val x = center.x + radius * (cos(lat) * sin(lon)).toFloat()
        val y = center.y - radius * sin(lat).toFloat()
        val r = radius * spot.size
        val width = 2 * r * cos(lon).toFloat().coerceAtLeast(0.25f)
        drawOval(color.copy(alpha = color.alpha * sqrt(facing)), Offset(x - width / 2, y - r), Size(width, 2 * r))
    }
}
