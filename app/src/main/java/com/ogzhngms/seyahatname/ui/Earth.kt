package com.ogzhngms.seyahatname.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ogzhngms.seyahatname.Place
import com.ogzhngms.seyahatname.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.launch

// NASA's Blue Marble imagery on a sphere whose centre faces the given latitude and longitude (radians),
// lit from the upper left with a dark night side, a blue haze toward the rim and a thin rim of air.
private const val EARTH_SHADER = """
uniform float2 size;
uniform float2 center;
uniform shader surface;
uniform float2 textureSize;

const float PI = 3.14159265;

half4 main(float2 coord) {
    float radius = min(size.x, size.y) * 0.4;
    float2 p = (coord - size * 0.5) / radius;
    float d = length(p);
    float edge = 1.5 / radius;
    float halo = smoothstep(1.07, 1.0, d);
    half4 glow = half4(0.28, 0.55, 1.0, 1.0) * half(halo * halo * 0.45);
    if (d > 1.0 + edge) return glow;

    float3 n = float3(p.x, -p.y, sqrt(max(0.0, 1.0 - dot(p, p))));
    float tilt = center.x;
    float3 t = float3(n.x, n.y * cos(tilt) + n.z * sin(tilt), -n.y * sin(tilt) + n.z * cos(tilt));
    float lat = asin(clamp(t.y, -1.0, 1.0));
    float lon = atan(t.x, t.z) + center.y;
    float2 uv = float2(fract(lon / (2.0 * PI) + 0.5), 0.5 - lat / PI);
    half3 color = surface.eval(uv * textureSize).rgb;
    // The source oceans are near-black navy; lift them toward the blue seen from orbit.
    half ocean = half(smoothstep(0.05, 0.25, float(color.b - max(color.r, color.g))));
    color = mix(color, color * half3(0.7, 1.3, 2.1) + half3(0.0, 0.04, 0.10), ocean);
    float light = clamp(dot(n, normalize(float3(-0.55, 0.45, 0.70))), 0.0, 1.0);
    float haze = pow(1.0 - n.z, 3.0);
    half3 rgb = color * half(0.16 + 0.95 * light) + half3(0.30, 0.55, 1.0) * half(haze * 0.65);
    return mix(half4(rgb, 1.0), glow, half(smoothstep(1.0 - edge, 1.0 + edge, d)));
}
"""

// Where the home-screen Earth starts: tilted so the north shows, Turkey facing the viewer.
private const val HOME_LAT = 0.40f
private const val HOME_LON = 0.61f
private const val FULL_TURN = (2 * PI).toFloat()

// The home-screen Earth, turning once a minute.
@Composable
fun SpinningEarth(modifier: Modifier = Modifier) {
    val turn by rememberInfiniteTransition(label = "turn").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing)),
        label = "turn",
    )
    Earth({ HOME_LAT }, { HOME_LON - turn * FULL_TURN }, modifier)
}

// The destination step's Earth: it turns slowly until the typed place is known, then flies there and drops a pin.
@Composable
fun DestinationEarth(place: Place?, modifier: Modifier = Modifier) {
    val lat = remember { Animatable(HOME_LAT) }
    val lon = remember { Animatable(HOME_LON) }
    LaunchedEffect(place) {
        if (place == null) {
            launch { lat.animateTo(HOME_LAT, tween(1200)) }
            while (true) lon.animateTo(lon.value - FULL_TURN, tween(60_000, easing = LinearEasing))
        } else {
            val targetLon = Math.toRadians(place.lon.toDouble()).toFloat()
            // Take the short way round.
            val nearest = targetLon + FULL_TURN * ((lon.value - targetLon) / FULL_TURN).roundToInt()
            launch { lat.animateTo(Math.toRadians(place.lat.toDouble()).toFloat(), tween(1200, easing = FastOutSlowInEasing)) }
            lon.animateTo(nearest, tween(1200, easing = FastOutSlowInEasing))
        }
    }
    val pin by animateFloatAsState(
        if (place != null) 1f else 0f,
        tween(if (place != null) 450 else 200, delayMillis = if (place != null) 1000 else 0, easing = FastOutSlowInEasing),
        label = "pin",
    )
    val ripple by rememberInfiniteTransition(label = "ripple").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_000, easing = LinearEasing)),
        label = "ripple",
    )
    val green = MaterialTheme.colorScheme.primary
    Box(modifier.aspectRatio(1f)) {
        Earth({ lat.value }, { lon.value }, Modifier.fillMaxSize())
        // The globe centres on the place, so the pin lands at the centre: it drops in, and rings spread
        // from its tip, flattened as if lying on the ground.
        Canvas(Modifier.fillMaxSize()) {
            if (pin == 0f) return@Canvas
            for (offset in listOf(0f, 0.5f)) {
                val t = (ripple + offset) % 1f
                val width = (8 + 56 * t).dp.toPx()
                drawOval(
                    green.copy(alpha = pin * (1 - t) * 0.8f),
                    Offset(center.x - width / 2, center.y - width / 6),
                    Size(width, width / 3),
                    style = Stroke(1.5.dp.toPx()),
                )
            }
            drawOval(Color.Black.copy(alpha = 0.35f * pin), Offset(center.x - 7.dp.toPx(), center.y - 2.5.dp.toPx()), Size(14.dp.toPx(), 5.dp.toPx()))
            val tip = center - Offset(0f, (1 - pin) * 24.dp.toPx())
            val head = tip - Offset(0f, 22.dp.toPx())
            val body = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(head.x - 8.dp.toPx(), head.y + 5.dp.toPx())
                lineTo(head.x + 8.dp.toPx(), head.y + 5.dp.toPx())
                close()
            }
            drawPath(body, green.copy(alpha = pin))
            drawCircle(green.copy(alpha = pin), 11.dp.toPx(), head)
            drawCircle(Color(0xFF0D1117).copy(alpha = pin), 4.dp.toPx(), head)
        }
    }
}

// Android 13+ draws the photographic shader; older phones get a simple Canvas globe.
@Composable
private fun Earth(lat: () -> Float, lon: () -> Float, modifier: Modifier) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ShaderEarth(lat, lon, modifier) else CanvasEarth(lon, modifier)
}

// Decoded once per process.
private var texture: Bitmap? = null

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderEarth(lat: () -> Float, lon: () -> Float, modifier: Modifier) {
    val resources = LocalContext.current.resources
    val shader = remember {
        RuntimeShader(EARTH_SHADER).apply {
            val bitmap = texture ?: BitmapFactory.decodeResource(resources, R.drawable.earth_texture).also { texture = it }
            val sampler = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP)
            sampler.filterMode = BitmapShader.FILTER_MODE_LINEAR
            setInputShader("surface", sampler)
            setFloatUniform("textureSize", bitmap.width.toFloat(), bitmap.height.toFloat())
        }
    }
    val brush = remember(shader) { ShaderBrush(shader) }
    Box(
        modifier.aspectRatio(1f).drawBehind {
            shader.setFloatUniform("size", size.width, size.height)
            shader.setFloatUniform("center", lat(), lon())
            drawRect(brush)
        },
    )
}

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

@Composable
private fun CanvasEarth(lon: () -> Float, modifier: Modifier) {
    Canvas(modifier.aspectRatio(1f)) {
        val radius = size.minDimension / 2 * 0.8f
        val outer = radius * 1.08f
        drawCircle(Brush.radialGradient(0.9f to Color(0x664FA3FF), 1f to Color.Transparent, center = center, radius = outer), outer)
        // Lit from the top left.
        drawCircle(Brush.radialGradient(listOf(Color(0xFF4FA3FF), Color(0xFF0D3B8C)), center - Offset(radius * 0.35f, radius * 0.35f), radius * 1.5f), radius)
        clipPath(Path().apply { addOval(Rect(center, radius)) }) { spots(radius, -Math.toDegrees(lon().toDouble()).toFloat()) }
        drawCircle(
            Brush.radialGradient(0.55f to Color.Transparent, 1f to Color(0xAA000000), center = center - Offset(radius * 0.3f, radius * 0.3f), radius = radius * 1.6f),
            radius,
        )
    }
}

// Orthographic projection of the turning sphere: spots on the far side are skipped,
// and spots near the edge get narrower and fainter.
private fun DrawScope.spots(radius: Float, turn: Float) {
    val color = Color(0xFF3DDC84)
    for (spot in LAND) {
        val lon = Math.toRadians((spot.lon + turn).toDouble())
        val lat = Math.toRadians(spot.lat.toDouble())
        val facing = (cos(lat) * cos(lon)).toFloat()
        if (facing <= 0f) continue
        val x = center.x + radius * (cos(lat) * sin(lon)).toFloat()
        val y = center.y - radius * sin(lat).toFloat()
        val r = radius * spot.size
        val width = 2 * r * cos(lon).toFloat().coerceAtLeast(0.25f)
        drawOval(color.copy(alpha = sqrt(facing)), Offset(x - width / 2, y - r), Size(width, 2 * r))
    }
}
