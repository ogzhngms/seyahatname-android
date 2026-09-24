package com.ogzhngms.seyahatname.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import com.ogzhngms.seyahatname.Planet
import com.ogzhngms.seyahatname.R
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

// Shared by the three shader planets: the disc, and the latitude/longitude of each point on the
// turning sphere after tilting its axis toward the viewer.
private const val SPHERE = """
uniform float2 size;
uniform float turn;

const float PI = 3.14159265;

float radius() { return min(size.x, size.y) * 0.4; }

float2 disc(float2 coord) { return (coord - size * 0.5) / radius(); }

float3 normal(float2 p) { return float3(p.x, -p.y, sqrt(max(0.0, 1.0 - dot(p, p)))); }

float2 latLon(float3 n, float tilt, float startLon) {
    float3 t = float3(n.x, n.y * cos(tilt) + n.z * sin(tilt), -n.y * sin(tilt) + n.z * cos(tilt));
    return float2(asin(clamp(t.y, -1.0, 1.0)), atan(t.x, t.z) + startLon - turn * 2.0 * PI);
}

float2 mapUv(float2 ll) { return float2(fract(ll.y / (2.0 * PI) + 0.5), 0.5 - ll.x / PI); }
"""

// NASA's Blue Marble imagery: tilted so the north shows, Turkey facing the viewer at start,
// lit from the upper left with a dark night side, a blue haze toward the rim and a glow.
private const val EARTH_SHADER = SPHERE + """
uniform shader surface;
uniform float2 textureSize;

half4 main(float2 coord) {
    float2 p = disc(coord);
    float d = length(p);
    float edge = 1.5 / radius();
    float halo = smoothstep(1.23, 1.0, d);
    half4 glow = half4(0.28, 0.55, 1.0, 1.0) * half(halo * halo * 0.55);
    if (d > 1.0 + edge) return glow;

    float3 n = normal(p);
    half3 color = surface.eval(mapUv(latLon(n, 0.40, 0.61)) * textureSize).rgb;
    // The source oceans are near-black navy; lift them toward the blue seen from orbit.
    half ocean = half(smoothstep(0.05, 0.25, float(color.b - max(color.r, color.g))));
    color = mix(color, color * half3(0.7, 1.3, 2.1) + half3(0.0, 0.04, 0.10), ocean);
    float light = clamp(dot(n, normalize(float3(-0.55, 0.45, 0.70))), 0.0, 1.0);
    float haze = pow(1.0 - n.z, 3.0);
    half3 rgb = color * half(0.16 + 0.95 * light) + half3(0.30, 0.55, 1.0) * half(haze * 0.65);
    return mix(half4(rgb, 1.0), glow, half(smoothstep(1.0 - edge, 1.0 + edge, d)));
}
"""

// NASA's LRO colour map with the near side facing the viewer. No air: a hard day/night line,
// near-black shadow and only a faint pale glow.
private const val MOON_SHADER = SPHERE + """
uniform shader surface;
uniform float2 textureSize;

half4 main(float2 coord) {
    float2 p = disc(coord);
    float d = length(p);
    float edge = 1.5 / radius();
    float halo = smoothstep(1.16, 1.0, d);
    half4 glow = half4(0.86, 0.88, 0.95, 1.0) * half(halo * halo * 0.16);
    if (d > 1.0 + edge) return glow;

    float3 n = normal(p);
    half3 color = surface.eval(mapUv(latLon(n, 0.10, 0.0)) * textureSize).rgb;
    float light = clamp(dot(n, normalize(float3(-0.55, 0.40, 0.73))), 0.0, 1.0);
    half3 rgb = color * half(0.03 + 1.3 * pow(light, 0.8));
    return mix(half4(rgb, 1.0), glow, half(smoothstep(1.0 - edge, 1.0 + edge, d)));
}
"""

// Drawn without an image: boiling granules from 3D noise on the turning sphere, darker limb and
// a breathing corona. The noise drifts around a loop, so the animation repeats without a jump.
private const val SUN_SHADER = SPHERE + """
float hash(float3 p) {
    p = fract(p * 0.3183099 + float3(0.71, 0.113, 0.419));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise(float3 x) {
    float3 i = floor(x);
    float3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(mix(hash(i), hash(i + float3(1.0, 0.0, 0.0)), f.x),
            mix(hash(i + float3(0.0, 1.0, 0.0)), hash(i + float3(1.0, 1.0, 0.0)), f.x), f.y),
        mix(mix(hash(i + float3(0.0, 0.0, 1.0)), hash(i + float3(1.0, 0.0, 1.0)), f.x),
            mix(hash(i + float3(0.0, 1.0, 1.0)), hash(i + float3(1.0, 1.0, 1.0)), f.x), f.y),
        f.z);
}

float fbm(float3 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 5; i++) {
        value += amplitude * noise(p);
        p *= 2.03;
        amplitude *= 0.5;
    }
    return value;
}

half4 main(float2 coord) {
    float2 p = disc(coord);
    float d = length(p);
    float edge = 1.5 / radius();
    float breathe = 1.0 + 0.06 * sin(turn * 2.0 * PI * 20.0);
    float corona = pow(clamp(1.0 - (d - 1.0) / (0.21 * breathe), 0.0, 1.0), 1.8);
    half4 glow = half4(1.0, 0.62, 0.18, 1.0) * half(corona * 0.9);
    if (d > 1.0 + edge) return glow;

    float3 n = normal(p);
    float2 ll = latLon(n, 0.12, 0.0);
    float3 q = float3(cos(ll.x) * sin(ll.y), sin(ll.x), cos(ll.x) * cos(ll.y));
    float a = turn * 2.0 * PI;
    float3 drift = float3(cos(a), sin(a), 0.0) * 0.6;
    // Fine granulation and larger cells only nudge the brightness; the colour comes from limb darkening:
    // a pale yellow centre that deepens to orange-red at the edge.
    float granules = fbm(q * 18.0 + drift);
    float cells = fbm(q * 4.0 - drift * 0.5);
    float mottle = 0.92 + 0.16 * (granules - 0.5) + 0.10 * (cells - 0.5);
    float mu = n.z;
    half3 base = mix(half3(0.96, 0.40, 0.04), half3(1.0, 0.88, 0.48), half(pow(mu, 0.8)));
    half3 rgb = base * half(mottle * (0.42 + 0.72 * pow(mu, 0.55)));
    return mix(half4(rgb, 1.0), glow, half(smoothstep(1.0 - edge, 1.0 + edge, d)));
}
"""

// Decoded once per process: the home screen and the profile previews share them.
private val textures = mutableMapOf<Int, Bitmap>()

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderPlanet(source: String, @DrawableRes texture: Int?, secondsPerTurn: Int, modifier: Modifier) {
    val resources = LocalContext.current.resources
    val shader = remember(source) {
        RuntimeShader(source).apply {
            if (texture != null) {
                val bitmap = textures.getOrPut(texture) { BitmapFactory.decodeResource(resources, texture) }
                val sampler = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP)
                sampler.filterMode = BitmapShader.FILTER_MODE_LINEAR
                setInputShader("surface", sampler)
                setFloatUniform("textureSize", bitmap.width.toFloat(), bitmap.height.toFloat())
            }
        }
    }
    val brush = remember(shader) { ShaderBrush(shader) }
    val turn by rememberInfiniteTransition(label = "turn").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(secondsPerTurn * 1000, easing = LinearEasing)),
        label = "turn",
    )
    Box(
        modifier.aspectRatio(1f).drawBehind {
            shader.setFloatUniform("size", size.width, size.height)
            shader.setFloatUniform("turn", turn)
            drawRect(brush)
        },
    )
}

// The home-screen planet: photographic Earth and Moon and a shader-drawn Sun on Android 13+;
// older phones get the Canvas drawing below, turning once every 40 seconds.
@Composable
fun PlanetView(planet: Planet, modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        when (planet) {
            Planet.EARTH -> ShaderPlanet(EARTH_SHADER, R.drawable.earth_texture, 60, modifier)
            Planet.MOON -> ShaderPlanet(MOON_SHADER, R.drawable.moon_texture, 90, modifier)
            Planet.SUN -> ShaderPlanet(SUN_SHADER, null, 120, modifier)
        }
        return
    }
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
