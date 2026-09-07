package dev.focusgarden.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import focusgarden.composeapp.generated.resources.Res
import focusgarden.composeapp.generated.resources.forest_autumn
import focusgarden.composeapp.generated.resources.forest_autumn_early
import focusgarden.composeapp.generated.resources.forest_night
import focusgarden.composeapp.generated.resources.forest_night_early
import focusgarden.composeapp.generated.resources.forest_sakura
import focusgarden.composeapp.generated.resources.forest_sakura_early
import focusgarden.composeapp.generated.resources.forest_serene
import focusgarden.composeapp.generated.resources.forest_serene_early
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LivingForest(biome: Biome, progress: Float, seed: Int, modifier: Modifier = Modifier) {
    val motion = rememberInfiniteTransition(label = "natural-atmosphere")
    val drift by motion.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Restart),
        label = "slow-drift",
    )
    val breathe by motion.animateFloat(
        .2f, 1f,
        infiniteRepeatable(tween(3_800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "light-breathe",
    )
    val rainfall by motion.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1_400, easing = LinearEasing), RepeatMode.Restart),
        label = "rainfall",
    )
    val flight by motion.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart),
        label = "bird-flight",
    )
    val wind by motion.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3_600, easing = LinearEasing), RepeatMode.Restart),
        label = "wind",
    )

    val matureImage = when (biome) {
        Biome.SERENE -> Res.drawable.forest_serene
        Biome.SAKURA -> Res.drawable.forest_sakura
        Biome.NIGHT -> Res.drawable.forest_night
        Biome.AUTUMN -> Res.drawable.forest_autumn
    }
    val earlyImage = when (biome) {
        Biome.SERENE -> Res.drawable.forest_serene_early
        Biome.SAKURA -> Res.drawable.forest_sakura_early
        Biome.NIGHT -> Res.drawable.forest_night_early
        Biome.AUTUMN -> Res.drawable.forest_autumn_early
    }
    val awakening = progress.coerceIn(0f, 1f)
    val matureAlpha = smoothStep(((awakening - .12f) / .78f).coerceIn(0f, 1f))

    Box(modifier.fillMaxSize()) {
        Image(
            painter = painterResource(earlyImage),
            contentDescription = biome.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Image(
            painter = painterResource(matureImage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(matureAlpha),
        )
        Canvas(Modifier.fillMaxSize()) {
            drawMovingAtmosphere(biome, awakening, seed, drift)
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(.18f), Color.Transparent, Color.Black.copy(.34f)),
                ),
            )

            // A chuva está viva no início e cede conforme a paisagem amadurece.
            if (biome != Biome.NIGHT && awakening < .48f) {
                val rainAlpha = ((.48f - awakening) / .48f).coerceIn(0f, 1f) * .17f
                repeat(52) { i ->
                    val x = (pseudo(seed + i * 31) * size.width + rainfall * 95f) % size.width
                    val cycle = (pseudo(seed + i * 47) + rainfall * (1.1f + pseudo(i) * .6f)) % 1f
                    val y = cycle * size.height
                    val length = 8f + pseudo(seed + i) * 13f
                    drawLine(
                        Color.White.copy(rainAlpha),
                        Offset(x, y), Offset(x - length * .18f, y + length),
                        .65f,
                    )
                }
            }

            // Pétalas e folhas surgem apenas quando a vegetação já amadureceu.
            if ((biome == Biome.SAKURA || biome == Biome.AUTUMN) && awakening > .58f) {
                val visibility = ((awakening - .58f) / .42f).coerceIn(0f, 1f)
                repeat(14) { i ->
                    val x = (pseudo(seed + i * 29) * size.width + drift * (30f + i * 3f)) % size.width
                    val y = ((pseudo(seed + i * 53) + rainfall * .18f) % 1f) * size.height
                    val color = if (biome == Biome.SAKURA) Color(0xFFF2D5D7) else Color(0xFFB8682E)
                    drawLine(color.copy(.22f * visibility), Offset(x, y), Offset(x + 3f, y + 5f), 1.4f)
                }
            }

            // Partículas discretas dão vida sem transformar a fotografia em ilustração.
            if (awakening > .68f) {
                val visibility = ((awakening - .68f) / .32f).coerceIn(0f, 1f)
                repeat(if (biome == Biome.NIGHT) 12 else 7) { i ->
                    val rawX = pseudo(seed + i * 37) * size.width
                    val x = (rawX + drift * (18f + i * 2f)) % size.width
                    val y = size.height * (.18f + pseudo(seed + i * 61) * .58f) +
                        sin((drift * PI * 2 + i).toFloat()) * 4f
                    val alpha = (.08f + breathe * .16f) * visibility
                    val color = if (biome == Biome.NIGHT) Color(0xFFFFE9A6) else Color.White
                    drawCircle(color.copy(alpha), 1f + pseudo(seed + i) * 1.5f, Offset(x, y))
                }
            }

            drawDistantBirds(biome, awakening, seed, flight, wind)
            drawInsects(biome, awakening, seed, drift, wind)
            drawWindLeaves(biome, awakening, seed, flight, wind)
            drawWindBlades(biome, awakening, seed, wind)
            if (biome == Biome.NIGHT) drawWaterGlints(awakening, seed, drift, breathe)
        }
    }
}

private fun DrawScope.drawWindLeaves(biome: Biome, progress: Float, seed: Int, flight: Float, wind: Float) {
    if (progress < .28f) return
    val visibility = ((progress - .28f) / .35f).coerceIn(0f, 1f)
    val count = if (biome == Biome.NIGHT) 7 else 13
    val color = when (biome) {
        Biome.SERENE -> Color(0xFF78915C)
        Biome.SAKURA -> Color(0xFFE9C8CC)
        Biome.AUTUMN -> Color(0xFFB9682E)
        Biome.NIGHT -> Color(0xFF314936)
    }
    repeat(count) { i ->
        val speed = .45f + pseudo(seed + i * 13) * .65f
        val cycle = (pseudo(seed + 1400 + i * 41) + flight * speed) % 1f
        val x = cycle * (size.width + 80f) - 40f
        val baseY = size.height * (.24f + pseudo(seed + 1500 + i * 37) * .48f)
        val y = baseY + sin((wind * PI * 4 + i * 1.3).toFloat()) * (6f + i % 4 * 2f)
        val tilt = sin((wind * PI * 8 + i).toFloat())
        drawLine(
            color.copy(.30f * visibility),
            Offset(x - 2.5f, y - tilt * 2f), Offset(x + 2.5f, y + tilt * 2f),
            1.5f,
        )
    }
}

private fun DrawScope.drawMovingAtmosphere(biome: Biome, progress: Float, seed: Int, drift: Float) {
    val mistAlpha = when (biome) {
        Biome.NIGHT -> .10f * (1f - progress * .55f)
        Biome.SERENE -> .07f * (1f - progress * .65f)
        else -> .035f
    }
    repeat(3) { i ->
        val width = size.width * (.42f + pseudo(seed + i * 17) * .24f)
        val x = ((pseudo(seed + i * 23) * (size.width + width)) + drift * (45f + i * 22f)) %
            (size.width + width) - width
        val y = size.height * (.18f + i * .15f)
        drawOval(
            Brush.radialGradient(
                listOf(Color.White.copy(mistAlpha), Color.Transparent),
                center = Offset(x + width / 2f, y), radius = width / 2f,
            ),
            topLeft = Offset(x, y - size.height * .07f),
            size = androidx.compose.ui.geometry.Size(width, size.height * .14f),
        )
    }
}

private fun DrawScope.drawDistantBirds(
    biome: Biome,
    progress: Float,
    seed: Int,
    flight: Float,
    wind: Float,
) {
    if (progress < .34f || biome == Biome.NIGHT) return
    val visibility = ((progress - .34f) / .22f).coerceIn(0f, 1f)
    repeat(5) { i ->
        val x = ((flight + pseudo(seed + i * 41)) % 1f) * (size.width + 100f) - 50f
        val y = size.height * (.14f + pseudo(seed + i * 19) * .18f) + sin((flight * PI * 4 + i).toFloat()) * 7f
        val wing = 3f + pseudo(seed + i * 7) * 4f
        val flap = sin((wind * PI * 10 + i).toFloat()) * wing * .45f
        val bird = Path().apply {
            moveTo(x - wing, y + flap)
            quadraticTo(x - wing * .45f, y - wing * .35f, x, y)
            quadraticTo(x + wing * .45f, y - wing * .35f, x + wing, y + flap)
        }
        drawPath(bird, Color(0xFF17201D).copy(.35f * visibility), style = Stroke(.9f))
    }
}

private fun DrawScope.drawInsects(biome: Biome, progress: Float, seed: Int, drift: Float, wind: Float) {
    if (progress < .62f || (biome != Biome.SERENE && biome != Biome.SAKURA)) return
    val visibility = ((progress - .62f) / .2f).coerceIn(0f, 1f)
    repeat(4) { i ->
        val centerX = pseudo(seed + 300 + i * 61) * size.width
        val centerY = size.height * (.55f + pseudo(seed + 500 + i * 43) * .27f)
        val x = centerX + sin((drift * PI * 6 + i * 1.7).toFloat()) * (13f + i * 3f)
        val y = centerY + cos((wind * PI * 4 + i).toFloat()) * (6f + i)
        drawCircle(Color.White.copy(.20f * visibility), 2.2f, Offset(x - 1.7f, y - 1.6f))
        drawCircle(Color.White.copy(.20f * visibility), 2.2f, Offset(x + 1.7f, y - 1.6f))
        drawOval(
            Color(0xFF2A2114).copy(.7f * visibility),
            Offset(x - 1.5f, y - 2.4f), androidx.compose.ui.geometry.Size(3f, 5f),
        )
        drawLine(Color(0xFFD39B35).copy(.8f * visibility), Offset(x - 1.3f, y), Offset(x + 1.3f, y), .8f)
    }
}

private fun DrawScope.drawWindBlades(biome: Biome, progress: Float, seed: Int, wind: Float) {
    if (progress < .18f || biome == Biome.NIGHT) return
    val amount = ((progress - .18f) / .5f).coerceIn(0f, 1f)
    val color = when (biome) {
        Biome.AUTUMN -> Color(0xFF8A672F)
        Biome.SAKURA -> Color(0xFF65764A)
        else -> Color(0xFF405E36)
    }
    repeat(22) { i ->
        val x = pseudo(seed + 800 + i * 31) * size.width
        val baseY = size.height * (.88f + pseudo(seed + 900 + i * 11) * .12f)
        val height = 8f + pseudo(seed + i * 17) * 18f
        val sway = sin((wind * PI * 2 + pseudo(i) * PI).toFloat()) * height * .22f
        drawLine(color.copy(.20f * amount), Offset(x, baseY), Offset(x + sway, baseY - height), .8f)
    }
}

private fun DrawScope.drawWaterGlints(progress: Float, seed: Int, drift: Float, breathe: Float) {
    if (progress < .42f) return
    val visibility = ((progress - .42f) / .4f).coerceIn(0f, 1f)
    repeat(18) { i ->
        val x = pseudo(seed + 1100 + i * 47) * size.width * .72f
        val y = size.height * (.63f + pseudo(seed + 1200 + i * 29) * .25f)
        val shimmer = .5f + .5f * sin((drift * PI * 8 + i).toFloat())
        val width = 4f + pseudo(seed + i) * 14f
        drawLine(Color(0xFFD7E5EE).copy(.08f * visibility * shimmer * breathe), Offset(x, y), Offset(x + width, y), .8f)
    }
}

private fun smoothStep(value: Float): Float = value * value * (3f - 2f * value)

internal fun pseudo(value: Int): Float {
    var x = value
    x = (x xor 61) xor (x ushr 16)
    x += x shl 3
    x = x xor (x ushr 4)
    x *= 0x27d4eb2d
    x = x xor (x ushr 15)
    return (x and 0x7fffffff) / Int.MAX_VALUE.toFloat()
}
