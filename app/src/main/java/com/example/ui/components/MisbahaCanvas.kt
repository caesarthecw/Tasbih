package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.example.ui.theme.WaterCyan
import com.example.ui.theme.WaterDeepBlue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class DropletParticle(
    val id: Long,
    val startXRatio: Float,
    val startYRatio: Float,
    val vx: Float,
    val ageSec: Float = 0f,
    val durationSec: Float = 0.72f,
    val radiusScale: Float = 1f,
)

private data class SplashRipple(
    val xRatio: Float,
    val ageSec: Float = 0f,
    val durationSec: Float = 0.55f,
)

@Composable
fun InteractiveMisbahaGardenStage(
    beadColor: Color,
    beadHighlight: Color,
    plantId: String,
    plantStageIndex: Int,
    stageProgress: Float,
    tapSequence: Int,
    hapticEnabled: Boolean,
    onTapMisbaha: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val slideAnim = remember { Animatable(0f) }
    val pulseAnim = remember { Animatable(0f) }
    val droplets = remember { mutableStateListOf<DropletParticle>() }
    val ripples = remember { mutableStateListOf<SplashRipple>() }
    var nextParticleId by remember { mutableLongStateOf(1L) }

    val infiniteTransition = rememberInfiniteTransition(label = "plantSway")
    val swayAngle by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sway",
    )

    // Trigger bead motion, pulse, and falling water droplets whenever tapSequence increments
    LaunchedEffect(tapSequence) {
        if (tapSequence > 0) {
            val id1 = nextParticleId++
            val id2 = nextParticleId++
            droplets.add(
                DropletParticle(
                    id = id1,
                    startXRatio = 0.50f + (Random.nextFloat() - 0.5f) * 0.05f,
                    startYRatio = 0.34f,
                    vx = (Random.nextFloat() - 0.5f) * 26f,
                    radiusScale = 1.08f,
                )
            )
            droplets.add(
                DropletParticle(
                    id = id2,
                    startXRatio = 0.50f + (Random.nextFloat() - 0.5f) * 0.08f,
                    startYRatio = 0.36f,
                    vx = (Random.nextFloat() - 0.5f) * 40f,
                    durationSec = 0.64f,
                    radiusScale = 0.72f,
                )
            )
            slideAnim.snapTo(1f)
            slideAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
    }

    LaunchedEffect(tapSequence) {
        if (tapSequence > 0) {
            pulseAnim.snapTo(1f)
            pulseAnim.animateTo(0f, tween(durationMillis = 360, easing = FastOutSlowInEasing))
        }
    }

    // 60fps particle physics loop while droplets or ripples are active
    LaunchedEffect(droplets.size, ripples.size) {
        if (droplets.isEmpty() && ripples.isEmpty()) return@LaunchedEffect
        var lastTime = withFrameNanos { it }
        while (droplets.isNotEmpty() || ripples.isNotEmpty()) {
            withFrameNanos { now ->
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastTime = now

                val iterator = droplets.listIterator()
                while (iterator.hasNext()) {
                    val d = iterator.next()
                    val nextAge = d.ageSec + dt
                    if (nextAge >= d.durationSec) {
                        iterator.remove()
                        ripples.add(SplashRipple(xRatio = d.startXRatio))
                    } else {
                        iterator.set(d.copy(ageSec = nextAge))
                    }
                }

                val rIter = ripples.listIterator()
                while (rIter.hasNext()) {
                    val r = rIter.next()
                    val nextAge = r.ageSec + dt
                    if (nextAge >= r.durationSec) {
                        rIter.remove()
                    } else {
                        rIter.set(r.copy(ageSec = nextAge))
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .semantics {
                role = Role.Button
                contentDescription = "المسبحة الإلكترونية، اضغط للتسبيح وسقي النبتة"
            }
            .pointerInput(hapticEnabled) {
                detectTapGestures(
                    onTap = {
                        if (hapticEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onTapMisbaha()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Draw curved rosary strand & moving beads in upper 48% of stage
            drawMisbahaBeadStrand(
                w = w,
                h = h,
                slideProgress = slideAnim.value,
                pulseProgress = pulseAnim.value,
                beadColor = beadColor,
                beadHighlight = beadHighlight,
                tapSequence = tapSequence,
            )

            // 2. Draw growing botanical plant & pot in lower 48% of stage
            val potTopY = h * 0.78f
            val soilY = potTopY + h * 0.022f
            drawGrowingPlantAndPot(
                w = w,
                h = h,
                potTopY = potTopY,
                plantId = plantId,
                stageIndex = plantStageIndex,
                stageProgress = stageProgress,
                sway = swayAngle,
                ripples = ripples,
            )

            // 3. Draw falling water droplets from bead strand to plant soil
            for (d in droplets) {
                val p = (d.ageSec / d.durationSec).coerceIn(0f, 1f)
                val easedY = p * p // gravity acceleration
                val startX = w * d.startXRatio
                val startY = h * d.startYRatio
                val endY = soilY
                val x = startX + d.vx * p
                val y = startY + (endY - startY) * easedY
                val alpha = (1f - p * 0.35f).coerceIn(0f, 1f)
                val dropRadius = (w.coerceAtMost(h) * 0.022f) * d.radiusScale

                drawWaterDroplet(center = Offset(x, y), radius = dropRadius, alpha = alpha)
            }
        }
    }
}

private fun DrawScope.drawMisbahaBeadStrand(
    w: Float,
    h: Float,
    slideProgress: Float,
    pulseProgress: Float,
    beadColor: Color,
    beadHighlight: Color,
    tapSequence: Int,
) {
    val cordCenterX = w * 0.5f
    val topY = h * 0.04f
    val bottomY = h * 0.42f
    val arcWidth = w * 0.32f

    // Subtle rosary cord arc
    val cordPath = Path().apply {
        moveTo(cordCenterX - arcWidth, topY)
        quadraticTo(cordCenterX, bottomY + h * 0.07f, cordCenterX + arcWidth, topY)
    }
    drawPath(
        path = cordPath,
        color = beadColor.copy(alpha = 0.38f),
        style = Stroke(width = 5f, cap = StrokeCap.Round),
    )

    // Render 9 beads along the quadratic curve; on tap they glide forward by one slot
    val visibleCount = 9
    val baseRadius = w.coerceAtMost(h) * 0.056f
    val shift = slideProgress // 1f -> 0f glides beads seamlessly into the next resting slot

    for (i in 0 until visibleCount) {
        val tRaw = (i + shift) / (visibleCount - 1).toFloat()
        val t = tRaw.coerceIn(0f, 1f)
        val inv = 1f - t
        val p0 = Offset(cordCenterX - arcWidth, topY)
        val p1 = Offset(cordCenterX, bottomY + h * 0.07f)
        val p2 = Offset(cordCenterX + arcWidth, topY)

        val bx = inv * inv * p0.x + 2f * inv * t * p1.x + t * t * p2.x
        val by = inv * inv * p0.y + 2f * inv * t * p1.y + t * t * p2.y

        val distFromCenter = kotlin.math.abs(t - 0.5f) * 2f
        val scale = 1.18f - 0.32f * distFromCenter
        val isFocal = i == visibleCount / 2
        val r = baseRadius * scale * (if (isFocal) 1f + pulseProgress * 0.14f else 1f)

        // Marker bead every 33rd position or alternating accent
        val isDividerBead = ((i + tapSequence) % 11 == 0)
        val fillBase = if (isDividerBead) beadHighlight else beadColor
        val fillDark = Color(
            red = (fillBase.red * 0.48f).coerceIn(0f, 1f),
            green = (fillBase.green * 0.48f).coerceIn(0f, 1f),
            blue = (fillBase.blue * 0.48f).coerceIn(0f, 1f),
            alpha = 1f,
        )

        if (isFocal && pulseProgress > 0.01f) {
            drawCircle(
                color = WaterCyan.copy(alpha = pulseProgress * 0.32f),
                radius = r * (1.35f + (1f - pulseProgress) * 0.45f),
                center = Offset(bx, by),
            )
        }

        // Soft drop shadow
        drawCircle(
            color = Color.Black.copy(alpha = 0.16f),
            radius = r * 0.98f,
            center = Offset(bx, by + r * 0.22f),
        )

        // 3D radial gradient bead
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.92f),
                    beadHighlight,
                    fillBase,
                    fillDark,
                ),
                center = Offset(bx - r * 0.28f, by - r * 0.32f),
                radius = r * 1.35f,
            ),
            radius = r,
            center = Offset(bx, by),
        )

        // Specular catchlight
        drawCircle(
            color = Color.White.copy(alpha = 0.55f),
            radius = r * 0.22f,
            center = Offset(bx - r * 0.30f, by - r * 0.32f),
        )
    }

    // Center tassel / Shahida drop cap underneath focal bead
    val tasselTop = Offset(cordCenterX, bottomY - h * 0.01f)
    drawCircle(
        color = beadHighlight.copy(alpha = 0.85f),
        radius = baseRadius * 0.28f,
        center = tasselTop,
    )
}

private fun DrawScope.drawWaterDroplet(center: Offset, radius: Float, alpha: Float) {
    val dropPath = Path().apply {
        moveTo(center.x, center.y - radius * 1.45f)
        cubicTo(
            center.x + radius * 1.15f, center.y - radius * 0.1f,
            center.x + radius * 1.05f, center.y + radius * 1.1f,
            center.x, center.y + radius * 1.1f,
        )
        cubicTo(
            center.x - radius * 1.05f, center.y + radius * 1.1f,
            center.x - radius * 1.15f, center.y - radius * 0.1f,
            center.x, center.y - radius * 1.45f,
        )
        close()
    }
    drawPath(
        path = dropPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.9f * alpha),
                WaterCyan.copy(alpha = 0.92f * alpha),
                WaterDeepBlue.copy(alpha = 0.95f * alpha),
            ),
            startY = center.y - radius * 1.4f,
            endY = center.y + radius * 1.1f,
        ),
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.65f * alpha),
        radius = radius * 0.22f,
        center = Offset(center.x - radius * 0.26f, center.y + radius * 0.2f),
    )
}

private fun DrawScope.drawGrowingPlantAndPot(
    w: Float,
    h: Float,
    potTopY: Float,
    plantId: String,
    stageIndex: Int,
    stageProgress: Float,
    sway: Float,
    ripples: List<SplashRipple>,
) {
    val cx = w * 0.5f
    val potWidthTop = w * 0.34f
    val potWidthBottom = w * 0.24f
    val potHeight = h * 0.18f
    val soilY = potTopY + h * 0.015f

    // Pot colors adapted slightly to plant species
    val potLight = when (plantId) {
        "plant_lotus" -> Color(0xFF4D8898)
        "plant_jasmine" -> Color(0xFFE7DFD3)
        else -> Color(0xFFC97852)
    }
    val potDark = when (plantId) {
        "plant_lotus" -> Color(0xFF285563)
        "plant_jasmine" -> Color(0xFFB3A490)
        else -> Color(0xFF8C4828)
    }

    // Pot shadow
    drawOval(
        color = Color.Black.copy(alpha = 0.14f),
        topLeft = Offset(cx - potWidthBottom * 0.65f, potTopY + potHeight - h * 0.01f),
        size = Size(potWidthBottom * 1.3f, h * 0.035f),
    )

    // Soil oval
    drawOval(
        color = Color(0xFF4A3222),
        topLeft = Offset(cx - potWidthTop * 0.48f, potTopY - h * 0.01f),
        size = Size(potWidthTop * 0.96f, h * 0.045f),
    )

    // Hydration ripples on soil when water droplets land
    for (r in ripples) {
        val p = (r.ageSec / r.durationSec).coerceIn(0f, 1f)
        val rx = cx + (r.xRatio - 0.5f) * potWidthTop * 0.5f
        val rw = potWidthTop * (0.14f + 0.45f * p)
        val rh = h * 0.016f * (1f + p)
        drawOval(
            color = WaterCyan.copy(alpha = (1f - p) * 0.65f),
            topLeft = Offset(rx - rw * 0.5f, soilY - rh * 0.5f),
            size = Size(rw, rh),
            style = Stroke(width = 3f),
        )
    }

    // Growth interpolation factor across stages (0f -> 1f smooth visual scaling)
    val totalVisualGrowth = ((stageIndex + stageProgress * 0.85f) / 5.5f).coerceIn(0.08f, 1f)
    val swayOffset = sway * (4f + stageIndex * 2.5f)

    // Draw plant body rising from soilY
    when (plantId) {
        "plant_lotus" -> drawLotusPlant(cx, soilY, h, stageIndex, totalVisualGrowth, swayOffset)
        "plant_palm" -> drawPalmPlant(cx, soilY, h, stageIndex, totalVisualGrowth, swayOffset)
        "plant_pomegranate" -> drawTreePlant(
            cx, soilY, h, stageIndex, totalVisualGrowth, swayOffset,
            leafColor = Color(0xFF3B8548),
            blossomColor = Color(0xFFC93B4B),
            showFruit = stageIndex >= 4,
        )
        "plant_jasmine" -> drawTreePlant(
            cx, soilY, h, stageIndex, totalVisualGrowth, swayOffset,
            leafColor = Color(0xFF2D7553),
            blossomColor = Color(0xFFFFFBF2),
            showFruit = stageIndex >= 3,
        )
        else -> drawTreePlant(
            cx, soilY, h, stageIndex, totalVisualGrowth, swayOffset,
            leafColor = Color(0xFF42844B),
            blossomColor = Color(0xFFD6E89C),
            showFruit = stageIndex >= 5,
        )
    }

    // Draw pot body in front of base of stem
    val potPath = Path().apply {
        moveTo(cx - potWidthTop * 0.5f, potTopY)
        lineTo(cx + potWidthTop * 0.5f, potTopY)
        lineTo(cx + potWidthBottom * 0.5f, potTopY + potHeight)
        lineTo(cx - potWidthBottom * 0.5f, potTopY + potHeight)
        close()
    }
    drawPath(
        path = potPath,
        brush = Brush.horizontalGradient(
            colors = listOf(potLight, potDark),
            startX = cx - potWidthTop * 0.5f,
            endX = cx + potWidthTop * 0.5f,
        ),
    )

    // Pot rim
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(potLight, potDark),
            startX = cx - potWidthTop * 0.54f,
            endX = cx + potWidthTop * 0.54f,
        ),
        topLeft = Offset(cx - potWidthTop * 0.54f, potTopY - h * 0.004f),
        size = Size(potWidthTop * 1.08f, h * 0.032f),
        cornerRadius = CornerRadius(14f, 14f),
    )
}

private fun DrawScope.drawTreePlant(
    cx: Float,
    soilY: Float,
    h: Float,
    stageIndex: Int,
    growth: Float,
    swayOffset: Float,
    leafColor: Color,
    blossomColor: Color,
    showFruit: Boolean,
) {
    if (stageIndex == 0) {
        // Seed nestled in soil + tiny crack
        drawOval(
            color = Color(0xFF8C6239),
            topLeft = Offset(cx - 12f, soilY - 10f),
            size = Size(24f, 16f),
        )
        if (growth > 0.09f) {
            drawLine(
                color = leafColor,
                start = Offset(cx, soilY - 6f),
                end = Offset(cx + swayOffset * 0.3f, soilY - 22f * (growth * 6f).coerceAtMost(1f)),
                strokeWidth = 5f,
                cap = StrokeCap.Round,
            )
        }
        return
    }

    val maxStemH = h * 0.34f * growth
    val topX = cx + swayOffset
    val topY = soilY - maxStemH
    val stemWidth = 6f + stageIndex * 2.2f

    // Curved trunk / stem
    val stemPath = Path().apply {
        moveTo(cx, soilY)
        quadraticTo(cx + swayOffset * 0.35f, soilY - maxStemH * 0.55f, topX, topY)
    }
    drawPath(
        path = stemPath,
        color = if (stageIndex >= 3) Color(0xFF6B4B32) else Color(0xFF4A8C4E),
        style = Stroke(width = stemWidth, cap = StrokeCap.Round),
    )

    // Stage 1+: twin sprout leaves
    val leafSize = 18f + stageIndex * 7f
    drawLeaf(Offset(topX, topY + maxStemH * 0.18f), -0.65f + swayOffset * 0.01f, leafSize, leafColor)
    drawLeaf(Offset(topX, topY + maxStemH * 0.18f), 0.65f + swayOffset * 0.01f, leafSize, leafColor)

    // Stage 2+: side branches & mid foliage
    if (stageIndex >= 2) {
        val midY = soilY - maxStemH * 0.55f
        val midX = cx + swayOffset * 0.45f
        drawLeaf(Offset(midX - 6f, midY), -1.0f, leafSize * 0.95f, leafColor.copy(alpha = 0.92f))
        drawLeaf(Offset(midX + 6f, midY), 1.0f, leafSize * 0.95f, leafColor.copy(alpha = 0.92f))
        drawLeaf(Offset(topX, topY), 0f + swayOffset * 0.015f, leafSize * 1.05f, leafColor)
    }

    // Stage 3+: fuller canopy clusters
    if (stageIndex >= 3) {
        val canopyR = 26f + stageIndex * 11f
        val branchLeft = Offset(topX - canopyR * 0.72f, topY + canopyR * 0.35f)
        val branchRight = Offset(topX + canopyR * 0.72f, topY + canopyR * 0.35f)

        drawLine(Color(0xFF6B4B32), Offset(cx + swayOffset * 0.55f, soilY - maxStemH * 0.62f), branchLeft, strokeWidth = stemWidth * 0.6f, cap = StrokeCap.Round)
        drawLine(Color(0xFF6B4B32), Offset(cx + swayOffset * 0.55f, soilY - maxStemH * 0.62f), branchRight, strokeWidth = stemWidth * 0.6f, cap = StrokeCap.Round)

        drawCircle(leafColor.copy(alpha = 0.88f), canopyR * 0.78f, branchLeft)
        drawCircle(leafColor.copy(alpha = 0.88f), canopyR * 0.78f, branchRight)
        drawCircle(leafColor, canopyR * 0.92f, Offset(topX, topY))
    }

    // Stage 4+: blossoms / fruits
    if (showFruit) {
        val spots = listOf(
            Offset(topX - 24f, topY + 6f),
            Offset(topX + 26f, topY + 10f),
            Offset(topX, topY - 14f),
            Offset(topX - 14f, topY + 24f),
        )
        for (spot in spots) {
            drawCircle(blossomColor, radius = if (stageIndex >= 5) 7.5f else 6f, center = spot)
            drawCircle(Color.White.copy(alpha = 0.5f), radius = 2.2f, center = Offset(spot.x - 2f, spot.y - 2f))
        }
    }
}

private fun DrawScope.drawLotusPlant(
    cx: Float,
    soilY: Float,
    h: Float,
    stageIndex: Int,
    growth: Float,
    swayOffset: Float,
) {
    // Water basin surface
    drawOval(
        color = WaterCyan.copy(alpha = 0.35f),
        topLeft = Offset(cx - 56f, soilY - 12f),
        size = Size(112f, 24f),
    )
    if (stageIndex == 0) {
        drawOval(Color(0xFF6E5739), Offset(cx - 10f, soilY - 8f), Size(20f, 14f))
        return
    }
    val stemH = h * 0.26f * growth
    val topX = cx + swayOffset
    val topY = soilY - stemH
    drawLine(Color(0xFF3E8E6E), Offset(cx, soilY), Offset(topX, topY), strokeWidth = 6f, cap = StrokeCap.Round)

    // Lily pads
    if (stageIndex >= 1) {
        drawOval(Color(0xFF358A65), Offset(cx - 42f, soilY - 10f), Size(38f, 14f))
        drawOval(Color(0xFF2D7656), Offset(cx + 6f, soilY - 8f), Size(36f, 13f))
    }

    // Lotus petals expanding by stage
    val petalCount = (stageIndex + 2).coerceAtMost(7)
    val petalLen = 20f + stageIndex * 5.5f
    val spread = 0.25f + stageIndex * 0.14f
    for (i in 0 until petalCount) {
        val ratio = if (petalCount == 1) 0f else (i.toFloat() / (petalCount - 1)) - 0.5f
        val angle = ratio * spread * PI.toFloat()
        val color = if (i % 2 == 0) Color(0xFFF4A4C0) else Color(0xFFFFDCE8)
        drawLeaf(Offset(topX, topY), angle, petalLen, color)
    }
    if (stageIndex >= 4) {
        drawCircle(Color(0xFFF6C445), radius = 8f, center = Offset(topX, topY - 8f))
    }
}

private fun DrawScope.drawPalmPlant(
    cx: Float,
    soilY: Float,
    h: Float,
    stageIndex: Int,
    growth: Float,
    swayOffset: Float,
) {
    if (stageIndex == 0) {
        drawOval(Color(0xFF6F4826), Offset(cx - 10f, soilY - 10f), Size(20f, 15f))
        return
    }
    val trunkH = h * 0.34f * growth
    val topX = cx + swayOffset
    val topY = soilY - trunkH

    val trunkPath = Path().apply {
        moveTo(cx, soilY)
        quadraticTo(cx - swayOffset * 0.4f, soilY - trunkH * 0.5f, topX, topY)
    }
    drawPath(
        path = trunkPath,
        color = Color(0xFF7B5434),
        style = Stroke(width = 8f + stageIndex * 2.5f, cap = StrokeCap.Round),
    )

    val frondCount = 3 + stageIndex
    val frondLen = 26f + stageIndex * 9f
    for (i in 0 until frondCount) {
        val angle = -1.3f + (2.6f * i / (frondCount - 1).coerceAtLeast(1))
        val endX = topX + sin(angle) * frondLen * 1.4f
        val endY = topY - cos(angle) * frondLen * 0.65f + 18f
        val frondPath = Path().apply {
            moveTo(topX, topY)
            quadraticTo(topX + sin(angle) * frondLen * 0.7f, topY - frondLen * 0.55f, endX, endY)
        }
        drawPath(
            path = frondPath,
            color = Color(0xFF2E7D4B),
            style = Stroke(width = 6f, cap = StrokeCap.Round),
        )
    }
    if (stageIndex >= 4) {
        // Golden date clusters
        drawCircle(Color(0xFFD4922A), radius = 6.5f, center = Offset(topX - 10f, topY + 12f))
        drawCircle(Color(0xFFD4922A), radius = 6.5f, center = Offset(topX + 10f, topY + 12f))
    }
}

private fun DrawScope.drawLeaf(
    base: Offset,
    angleRad: Float,
    length: Float,
    color: Color,
) {
    val tipX = base.x + sin(angleRad) * length
    val tipY = base.y - cos(angleRad) * length
    val perpX = cos(angleRad) * length * 0.38f
    val perpY = sin(angleRad) * length * 0.38f
    val midX = (base.x + tipX) * 0.5f
    val midY = (base.y + tipY) * 0.5f

    val path = Path().apply {
        moveTo(base.x, base.y)
        quadraticTo(midX - perpX, midY - perpY, tipX, tipY)
        quadraticTo(midX + perpX, midY + perpY, base.x, base.y)
        close()
    }
    drawPath(path = path, color = color)
}
