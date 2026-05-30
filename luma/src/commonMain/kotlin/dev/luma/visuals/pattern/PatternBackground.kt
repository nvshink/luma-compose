package dev.luma.visuals.pattern

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.luma.visuals.model.Falloff
import dev.luma.visuals.model.OffsetFraction
import dev.luma.visuals.pattern.animation.PatternInteraction
import dev.luma.visuals.pattern.animation.PatternRotation
import dev.luma.visuals.pattern.animation.RotationDirection
import dev.luma.visuals.pattern.layout.PatternLayoutResolver
import dev.luma.visuals.pattern.layout.PatternLayoutScope
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Draws a configurable element-based background using layouts, interaction and motion models.
 *
 * @param state Runtime animation and interaction inputs.
 * @param style Visual configuration for layout, element rendering and effects.
 * @param modifier Modifier applied to the drawing container.
 */
@Composable
fun PatternBackground(
    state: PatternState,
    style: PatternStyle,
    modifier: Modifier = Modifier,
) {
    val latestState = rememberUpdatedState(state)
    val frameClock = rememberPatternFrameClock(state)
    Spacer(
        modifier = modifier.drawWithCache {
            val elementSize = style.element.size.toPxSize(this)
            val positions = PatternLayoutResolver.resolve(
                layout = style.layout,
                scope = PatternLayoutScope(
                    containerSize = size,
                    elementSize = elementSize,
                    density = this,
                ),
            )
            val renderBudget = resolveRenderBudget(positions.size)
            val preparedBlur = prepareBlur(style.blur, style.scale, elementSize, this, renderBudget)
            val preparedShadow = prepareShadow(style.shadow, style.scale, this, renderBudget)
            val preparedInteraction = prepareInteraction(style.interaction, this)
            val preparedDepth = prepareDepth(style.depth, this)
            val preparedRotation = prepareRotation(style.rotation, positions.size)
            val preparedElement = prepareElement(
                element = style.element,
                elementSize = elementSize,
                baseScale = style.scale,
                blur = preparedBlur,
                shadow = preparedShadow,
                layoutDirection = layoutDirection,
                density = this,
            )
            val mainPass = preparedElement.preparePass(style.elementTint ?: style.tint, style.alpha)
            val shadowPass = (preparedShadow as? PreparedPatternShadow.Drop)?.let {
                preparedElement.preparePass(style.shadowTint ?: it.color.copy(alpha = 1f), style.alpha * it.color.alpha)
            }
            onDrawBehind {
                val currentState = latestState.value
                val interactionPoint = resolveInteractionPoint(style.interaction, currentState)?.toOffset(size)
                val containerCenter = Offset(size.width / 2f, size.height / 2f)
                val interactionVector = interactionPoint?.minus(containerCenter) ?: Offset.Zero
                val drawState = PreparedPatternDrawState(
                    interactionPoint = interactionPoint,
                    containerSize = size,
                    containerCenter = containerCenter,
                    containerMaxDimension = max(size.width, size.height).coerceAtLeast(1f),
                    animationTimeMillis = currentState.resolveAnimationTimeMillis(frameClock),
                    baseRotationDegrees = currentState.baseRotationDegrees,
                )
                val uniformOffset = resolveUniformInteractionOffset(preparedInteraction, interactionVector)
                val uniformInteractionAmount = resolveUniformInteractionAmount(preparedInteraction)
                val depthVector = interactionVector.normalize().takeIf { it != Offset.Zero }
                val uniformRotationDegrees = resolveUniformRotation(preparedRotation, drawState.animationTimeMillis)
                positions.forEachIndexed { index, position ->
                    drawPatternNode(
                        index = index,
                        basePosition = position,
                        preparedElement = preparedElement,
                        preparedBlur = preparedBlur,
                        preparedShadow = preparedShadow,
                        preparedInteraction = preparedInteraction,
                        preparedDepth = preparedDepth,
                        preparedRotation = preparedRotation,
                        drawState = drawState,
                        mainPass = mainPass,
                        shadowPass = shadowPass,
                        uniformOffset = uniformOffset,
                        uniformInteractionAmount = uniformInteractionAmount,
                        depthVector = depthVector,
                        uniformRotationDegrees = uniformRotationDegrees,
                    )
                }
            }
        },
    )
}

@Stable
private class PatternFrameClock {
    var timeMillis by mutableLongStateOf(0L)
}

@Composable
private fun rememberPatternFrameClock(state: PatternState): PatternFrameClock {
    val frameClock = remember { PatternFrameClock() }
    val usesInternalClock = state.isAnimated && state.animationTimeMillis == 0L
    LaunchedEffect(usesInternalClock) {
        if (!usesInternalClock) return@LaunchedEffect
        while (true) {
            withFrameNanos { frameClock.timeMillis = it / 1_000_000L }
        }
    }
    return frameClock
}

private fun DrawScope.drawPatternNode(
    index: Int,
    basePosition: Offset,
    preparedElement: PreparedPatternElement,
    preparedBlur: PreparedPatternBlur,
    preparedShadow: PreparedPatternShadow,
    preparedInteraction: PreparedPatternInteraction,
    preparedDepth: PreparedPatternDepth,
    preparedRotation: PreparedPatternRotation,
    drawState: PreparedPatternDrawState,
    mainPass: PreparedElementPass,
    shadowPass: PreparedElementPass?,
    uniformOffset: Offset,
    uniformInteractionAmount: Float,
    depthVector: Offset?,
    uniformRotationDegrees: Float?,
) {
    val interaction = resolveInteraction(
        interaction = preparedInteraction,
        interactionPoint = drawState.interactionPoint,
        elementPosition = basePosition,
        containerCenter = drawState.containerCenter,
        uniformOffset = uniformOffset,
        uniformAmount = uniformInteractionAmount,
    )
    val depthOffset = resolveDepthOffset(
        depth = preparedDepth,
        interactionPoint = drawState.interactionPoint,
        basePosition = basePosition,
        containerCenter = drawState.containerCenter,
        containerMaxDimension = drawState.containerMaxDimension,
        depthVector = depthVector,
    )
    val center = basePosition + interaction.offset + depthOffset
    val rotation = drawState.baseRotationDegrees + (
        uniformRotationDegrees ?: resolveRotation(
            rotation = preparedRotation,
            timeMillis = drawState.animationTimeMillis,
            index = index,
            basePosition = center,
            interactionPoint = drawState.interactionPoint,
            containerSize = drawState.containerSize,
        )
    )

    when (preparedShadow) {
        PreparedPatternShadow.None -> Unit
        is PreparedPatternShadow.Drop -> {
            val shadowOffset = resolveShadowOffset(
                shadow = preparedShadow,
                interactionPoint = drawState.interactionPoint,
                elementPosition = center,
                containerMaxDimension = drawState.containerMaxDimension,
            )
            drawProjectedShadow(
                preparedElement = preparedElement,
                center = center,
                rotationDegrees = rotation,
                pass = shadowPass ?: return,
                shadow = preparedShadow,
                shadowOffset = shadowOffset,
                interactionAmount = interaction.amount,
                nodeIndex = index,
            )
        }
    }

    when (preparedBlur) {
        PreparedPatternBlur.None -> Unit
        is PreparedPatternBlur.Soft -> {
            repeat(preparedBlur.passes) { pass ->
                val factor = (preparedBlur.passes - pass).coerceAtLeast(1)
                val blurAlpha = mainPass.alpha * preparedBlur.alphaMultiplier / factor
                if (blurAlpha <= MinimumVisibleAlpha) return@repeat
                drawPatternElement(
                    preparedElement = preparedElement,
                    center = center,
                    rotationDegrees = rotation,
                    scale = preparedBlur.scales[pass],
                    pass = mainPass,
                    alpha = blurAlpha,
                    interactionAmount = interaction.amount,
                    nodeIndex = index,
                    isShadowPass = false,
                )
            }
        }
    }

    drawPatternElement(
        preparedElement = preparedElement,
        center = center,
        rotationDegrees = rotation,
        scale = preparedElement.baseScale,
        pass = mainPass,
        alpha = mainPass.alpha,
        interactionAmount = interaction.amount,
        nodeIndex = index,
        isShadowPass = false,
    )
}

private fun DrawScope.drawProjectedShadow(
    preparedElement: PreparedPatternElement,
    center: Offset,
    rotationDegrees: Float,
    pass: PreparedElementPass,
    shadow: PreparedPatternShadow.Drop,
    shadowOffset: Offset,
    interactionAmount: Float,
    nodeIndex: Int,
) {
    val projectedOffset = shadowOffset * shadow.height
    repeat(shadow.passCount) { shadowPassIndex ->
        val progress = (shadowPassIndex + 1) / shadow.passCount.toFloat()
        val easedProgress = 1f - ((1f - progress) * (1f - progress))
        val shadowAlpha = (pass.alpha * (1.15f - progress) / shadow.passCount * 1.9f).coerceIn(0f, 1f)
        if (shadowAlpha <= MinimumVisibleAlpha) return@repeat
        drawPatternElement(
            preparedElement = preparedElement,
            center = center + (projectedOffset * easedProgress),
            rotationDegrees = rotationDegrees,
            scale = shadow.scales[shadowPassIndex],
            pass = pass,
            alpha = shadowAlpha,
            interactionAmount = interactionAmount,
            nodeIndex = nodeIndex,
            isShadowPass = true,
        )
    }
}

private fun DrawScope.drawPatternElement(
    preparedElement: PreparedPatternElement,
    center: Offset,
    rotationDegrees: Float,
    scale: Float,
    pass: PreparedElementPass,
    alpha: Float,
    interactionAmount: Float,
    nodeIndex: Int,
    isShadowPass: Boolean,
) {
    if (alpha <= MinimumVisibleAlpha) return
    val element = preparedElement.element
    val clampedScale = scale.coerceAtLeast(0f)
    val drawSize = Size(
        width = preparedElement.elementSize.width * clampedScale,
        height = preparedElement.elementSize.height * clampedScale,
    )
    withTransform({
        translate(left = center.x - drawSize.width / 2f, top = center.y - drawSize.height / 2f)
        rotate(degrees = rotationDegrees, pivot = Offset(drawSize.width / 2f, drawSize.height / 2f))
    }) {
        when (element) {
            is PatternElement.Dot -> {
                drawCircle(
                    color = pass.color ?: element.color,
                    radius = drawSize.minDimension / 2f,
                    center = Offset(drawSize.width / 2f, drawSize.height / 2f),
                    alpha = alpha.coerceIn(0f, 1f),
                )
            }

            is PatternElement.Shape -> {
                val outline = with(preparedElement) {
                    outlineFor(clampedScale, drawSize, layoutDirection, this@drawPatternElement, element.shape)
                }
                drawShapeOutline(
                    outline = outline,
                    color = pass.color ?: element.color,
                    alpha = alpha.coerceIn(0f, 1f),
                )
            }

            is PatternElement.Sprite -> {
                with(element.painter) {
                    draw(
                        size = drawSize,
                        alpha = alpha.coerceIn(0f, 1f),
                        colorFilter = pass.colorFilter,
                    )
                }
            }

            is PatternElement.Custom -> {
                element.draw.invoke(
                    this,
                    PatternElementScope(
                        nodeIndex = nodeIndex,
                        size = drawSize,
                        alpha = alpha.coerceIn(0f, 1f),
                        rotationDegrees = rotationDegrees,
                        interactionAmount = interactionAmount,
                        isShadowPass = isShadowPass,
                        tint = pass.color,
                    ),
                )
            }
        }
    }
}

private fun DrawScope.drawShapeOutline(
    outline: Outline,
    color: Color,
    alpha: Float,
) {
    when (outline) {
        is Outline.Rectangle -> drawRect(color = color, topLeft = outline.rect.topLeft, size = outline.rect.size, alpha = alpha)
        is Outline.Rounded -> drawRoundRect(
            color = color,
            topLeft = Offset(outline.roundRect.left, outline.roundRect.top),
            size = Size(outline.roundRect.width, outline.roundRect.height),
            cornerRadius = outline.roundRect.topLeftCornerRadius,
            alpha = alpha,
        )
        is Outline.Generic -> drawPath(path = outline.path, color = color, alpha = alpha, style = Fill)
    }
}

private fun resolveInteractionPoint(interaction: PatternInteraction, state: PatternState): OffsetFraction? = when (interaction) {
    PatternInteraction.None -> state.interactionPoint
    is PatternInteraction.DistanceBasedOffset -> state.interactionPoint ?: interaction.point
    is PatternInteraction.UniformOffset -> state.interactionPoint ?: interaction.point
}

private data class InteractionResolution(
    val offset: Offset,
    val amount: Float,
)

private fun resolveInteraction(
    interaction: PreparedPatternInteraction,
    interactionPoint: Offset?,
    elementPosition: Offset,
    containerCenter: Offset,
    uniformOffset: Offset,
    uniformAmount: Float,
): InteractionResolution {
    if (interactionPoint == null) return InteractionResolution(Offset.Zero, 0f)
    return when (interaction) {
        PreparedPatternInteraction.None -> InteractionResolution(Offset.Zero, 0f)
        is PreparedPatternInteraction.UniformOffset -> InteractionResolution(offset = uniformOffset, amount = uniformAmount)

        is PreparedPatternInteraction.DistanceBasedOffset -> {
            val direction = elementPosition - interactionPoint
            val distance = direction.getDistance()
            val influence = interaction.falloff.transform((distance / interaction.radiusPx).coerceIn(0f, 1f)) * interaction.intensity
            InteractionResolution(
                offset = direction.normalize() * interaction.maxOffsetPx * influence,
                amount = influence.coerceIn(0f, 1f),
            )
        }
    }
}

private fun resolveDepthOffset(
    depth: PreparedPatternDepth,
    interactionPoint: Offset?,
    basePosition: Offset,
    containerCenter: Offset,
    containerMaxDimension: Float,
    depthVector: Offset?,
): Offset {
    if (depth == PreparedPatternDepth.None || interactionPoint == null) return Offset.Zero
    depth as PreparedPatternDepth.Parallax
    val vector = depthVector ?: (interactionPoint - containerCenter).normalize()
    val distanceFactor = (basePosition - containerCenter).getDistance() / containerMaxDimension
    return vector * depth.maxOffsetPx * depth.intensity * distanceFactor
}

private fun resolveShadowOffset(
    shadow: PreparedPatternShadow.Drop,
    interactionPoint: Offset?,
    elementPosition: Offset,
    containerMaxDimension: Float,
): Offset {
    val base = shadow.baseOffset
    if (!shadow.useInteractionAsLight || interactionPoint == null) return base
    val oppositeDirection = elementPosition - interactionPoint
    val distance = oppositeDirection.getDistance()
    if (distance <= 0.0001f) return Offset.Zero

    val normalizedDistance = (distance / containerMaxDimension).coerceIn(0f, 1f)
    val maxShadowLength = max(base.getDistance(), shadow.blurRadiusPx * 0.75f)
    return oppositeDirection.normalize() * (maxShadowLength * normalizedDistance)
}

private fun resolveRotation(
    rotation: PreparedPatternRotation,
    timeMillis: Long,
    index: Int,
    basePosition: Offset,
    interactionPoint: Offset?,
    containerSize: Size,
): Float {
    val seconds = timeMillis / 1_000f
    return when (rotation) {
        PreparedPatternRotation.None -> 0f
        is PreparedPatternRotation.Uniform -> seconds * rotation.degreesPerSecond * rotation.directionSign
        is PreparedPatternRotation.Async -> seconds * rotation.speeds[index] * rotation.directionSign
        is PreparedPatternRotation.Distributed -> seconds * rotation.speeds[index] * rotation.directionSign
        is PreparedPatternRotation.Follow -> {
            val wobbleAmplitude = rotation.wobbleAmplitudes[index]
            val wobbleSpeed = rotation.wobbleSpeeds[index]
            if (wobbleAmplitude == 0f || wobbleSpeed == 0f) {
                0f
            } else {
                val angleRadians = ((seconds * wobbleSpeed * 360f + rotation.phases[index]) * PI.toFloat()) / 180f
                sin(angleRadians) * wobbleAmplitude
            }
        }
        is PreparedPatternRotation.Directed -> {
            val target = if (rotation.useInteractionPoint) {
                interactionPoint ?: rotation.defaultPoint.toOffset(containerSize)
            } else {
                rotation.defaultPoint.toOffset(containerSize)
            }
            val vector = target - basePosition
            if (vector.getDistance() <= 0.0001f) {
                rotation.angleOffsetDegrees
            } else {
                (atan2(vector.y, vector.x) * 180f / PI.toFloat()) + rotation.angleOffsetDegrees
            }
        }
    }
}

private fun resolveUniformInteractionOffset(
    interaction: PreparedPatternInteraction,
    interactionVector: Offset,
): Offset = when (interaction) {
    PreparedPatternInteraction.None -> Offset.Zero
    is PreparedPatternInteraction.UniformOffset -> interactionVector.normalize() * interaction.maxOffsetPx
    is PreparedPatternInteraction.DistanceBasedOffset -> Offset.Zero
}

private fun resolveUniformInteractionAmount(interaction: PreparedPatternInteraction): Float = when (interaction) {
    PreparedPatternInteraction.None -> 0f
    is PreparedPatternInteraction.UniformOffset -> interaction.intensity
    is PreparedPatternInteraction.DistanceBasedOffset -> 0f
}

private fun resolveUniformRotation(
    rotation: PreparedPatternRotation,
    timeMillis: Long,
): Float? {
    val seconds = timeMillis / 1_000f
    return when (rotation) {
        PreparedPatternRotation.None -> 0f
        is PreparedPatternRotation.Uniform -> seconds * rotation.degreesPerSecond * rotation.directionSign
        else -> null
    }
}

private fun prepareInteraction(interaction: PatternInteraction, density: Density): PreparedPatternInteraction = with(density) {
    when (interaction) {
        PatternInteraction.None -> PreparedPatternInteraction.None
        is PatternInteraction.UniformOffset -> PreparedPatternInteraction.UniformOffset(
            maxOffsetPx = interaction.maxOffset.toPx() * interaction.intensity.coerceAtLeast(0f),
            intensity = interaction.intensity.coerceIn(0f, 1f),
        )
        is PatternInteraction.DistanceBasedOffset -> PreparedPatternInteraction.DistanceBasedOffset(
            maxOffsetPx = interaction.maxOffset.toPx(),
            intensity = interaction.intensity.coerceAtLeast(0f),
            radiusPx = interaction.radius.toPx().coerceAtLeast(1f),
            falloff = interaction.falloff,
        )
    }
}

private fun prepareDepth(depth: PatternDepth, density: Density): PreparedPatternDepth = with(density) {
    when (depth) {
        PatternDepth.None -> PreparedPatternDepth.None
        is PatternDepth.Parallax -> PreparedPatternDepth.Parallax(
            maxOffsetPx = depth.maxOffset.toPx(),
            intensity = depth.intensity,
        )
    }
}

private fun prepareBlur(
    blur: PatternBlur,
    baseScale: Float,
    elementSize: Size,
    density: Density,
    renderBudget: PatternRenderBudget,
): PreparedPatternBlur = with(density) {
    when (blur) {
        PatternBlur.None -> PreparedPatternBlur.None
        is PatternBlur.Soft -> {
            val passCount = (blur.passes.coerceAtLeast(1) * renderBudget.blurPassMultiplier).roundToInt().coerceAtLeast(1)
            val radiusPx = blur.radius.toPx()
            val scaleBoost = 1f + (radiusPx / elementSize.minDimension.coerceAtLeast(1f))
            val scales = FloatArray(passCount) { pass ->
                val factor = (passCount - pass).coerceAtLeast(1)
                baseScale * (1f + ((scaleBoost - 1f) * factor / passCount))
            }
            PreparedPatternBlur.Soft(
                alphaMultiplier = blur.alphaMultiplier,
                passes = passCount,
                scales = scales,
            )
        }
    }
}

private fun prepareShadow(
    shadow: PatternShadow,
    baseScale: Float,
    density: Density,
    renderBudget: PatternRenderBudget,
): PreparedPatternShadow = with(density) {
    when (shadow) {
        PatternShadow.None -> PreparedPatternShadow.None
        is PatternShadow.Drop -> {
            val height = shadow.height.coerceAtLeast(0f)
            val blurRadiusPx = shadow.blurRadius.toPx()
            val basePassCount = (2f + height * 2f + (blurRadiusPx / 28f)).roundToInt().coerceIn(2, 7)
            val passCount = (basePassCount * renderBudget.shadowPassMultiplier).roundToInt().coerceIn(1, 6)
            val scales = FloatArray(passCount) { pass ->
                val progress = (pass + 1) / passCount.toFloat()
                val easedProgress = 1f - ((1f - progress) * (1f - progress))
                baseScale * lerp(1f, shadow.spread, easedProgress)
            }
            PreparedPatternShadow.Drop(
                color = shadow.color,
                blurRadiusPx = blurRadiusPx,
                baseOffset = Offset(shadow.offset.x.toPx(), shadow.offset.y.toPx()),
                height = height,
                useInteractionAsLight = shadow.useInteractionAsLight,
                passCount = passCount,
                scales = scales,
            )
        }
    }
}

private fun prepareRotation(rotation: PatternRotation, count: Int): PreparedPatternRotation = when (rotation) {
    PatternRotation.None -> PreparedPatternRotation.None
    is PatternRotation.Uniform -> PreparedPatternRotation.Uniform(
        degreesPerSecond = rotation.degreesPerSecond,
        directionSign = rotation.direction.sign,
    )
    is PatternRotation.Async -> {
        val speeds = FloatArray(count) { index ->
            val random = Random(rotation.seed + index)
            rotation.baseDegreesPerSecond * (1f + random.nextFloat() * rotation.speedMultiplierVariation.coerceAtLeast(0f))
        }
        PreparedPatternRotation.Async(speeds = speeds, directionSign = rotation.direction.sign)
    }
    is PatternRotation.Distributed -> {
        val speeds = FloatArray(count) { index ->
            val random = Random(rotation.seed + index)
            rotation.baseDegreesPerSecond + random.nextFloat() * rotation.variation * if (index % 2 == 0) 1f else -1f
        }
        PreparedPatternRotation.Distributed(speeds = speeds, directionSign = rotation.direction.sign)
    }
    is PatternRotation.Follow -> {
        val wobbleAmplitudes = FloatArray(count)
        val wobbleSpeeds = FloatArray(count)
        val phases = FloatArray(count)
        repeat(count) { index ->
            val random = Random(rotation.seed + index)
            wobbleAmplitudes[index] = rotation.wobbleAmplitudeDegrees + random.nextFloat() * rotation.variation
            wobbleSpeeds[index] = rotation.wobbleSpeedMultiplier + random.nextFloat() * rotation.variation * 0.1f
            phases[index] = random.nextFloat() * 360f
        }
        PreparedPatternRotation.Follow(
            wobbleAmplitudes = wobbleAmplitudes,
            wobbleSpeeds = wobbleSpeeds,
            phases = phases,
        )
    }
    is PatternRotation.Directed -> PreparedPatternRotation.Directed(
        defaultPoint = rotation.point.clamped(),
        useInteractionPoint = rotation.useInteractionPoint,
        angleOffsetDegrees = rotation.angleOffsetDegrees,
    )
}

private fun prepareElement(
    element: PatternElement,
    elementSize: Size,
    baseScale: Float,
    blur: PreparedPatternBlur,
    shadow: PreparedPatternShadow,
    layoutDirection: LayoutDirection,
    density: Density,
): PreparedPatternElement {
    if (element !is PatternElement.Shape) {
        return PreparedPatternElement(element = element, elementSize = elementSize, baseScale = baseScale)
    }

    val scales = linkedSetOf(baseScale.scaleKey())
    if (blur is PreparedPatternBlur.Soft) {
        blur.scales.forEach { scales += it.scaleKey() }
    }
    if (shadow is PreparedPatternShadow.Drop) {
        shadow.scales.forEach { scales += it.scaleKey() }
    }

    val outlineCache = HashMap<Int, Outline>(scales.size)
    scales.forEach { scaleKey ->
        val scale = Float.fromBits(scaleKey)
        val drawSize = Size(width = elementSize.width * scale, height = elementSize.height * scale)
        outlineCache[scaleKey] = element.shape.createOutline(drawSize, layoutDirection, density)
    }
    return PreparedPatternElement(
        element = element,
        elementSize = elementSize,
        baseScale = baseScale,
        outlineCache = outlineCache,
    )
}

private fun resolveRenderBudget(nodeCount: Int): PatternRenderBudget = when {
    nodeCount >= 220 -> PatternRenderBudget(shadowPassMultiplier = 0.18f, blurPassMultiplier = 0.28f)
    nodeCount >= 160 -> PatternRenderBudget(shadowPassMultiplier = 0.26f, blurPassMultiplier = 0.36f)
    nodeCount >= 120 -> PatternRenderBudget(shadowPassMultiplier = 0.38f, blurPassMultiplier = 0.5f)
    nodeCount >= 80 -> PatternRenderBudget(shadowPassMultiplier = 0.55f, blurPassMultiplier = 0.68f)
    nodeCount >= 48 -> PatternRenderBudget(shadowPassMultiplier = 0.78f, blurPassMultiplier = 0.84f)
    else -> PatternRenderBudget(shadowPassMultiplier = 1f, blurPassMultiplier = 1f)
}

private fun PatternState.resolveAnimationTimeMillis(frameClock: PatternFrameClock): Long = when {
    animationTimeMillis != 0L -> animationTimeMillis
    isAnimated -> frameClock.timeMillis
    else -> 0L
}

private data class PreparedPatternDrawState(
    val interactionPoint: Offset?,
    val containerSize: Size,
    val containerCenter: Offset,
    val containerMaxDimension: Float,
    val animationTimeMillis: Long,
    val baseRotationDegrees: Float,
)

private data class PreparedElementPass(
    val color: Color?,
    val colorFilter: ColorFilter?,
    val alpha: Float,
)

private data class PreparedPatternElement(
    val element: PatternElement,
    val elementSize: Size,
    val baseScale: Float,
    val outlineCache: Map<Int, Outline> = emptyMap(),
) {
    fun DrawScope.outlineFor(
        scale: Float,
        drawSize: Size,
        layoutDirection: LayoutDirection,
        density: Density,
        shape: Shape,
    ): Outline = outlineCache[scale.scaleKey()] ?: shape.createOutline(drawSize, layoutDirection, density)

    fun resolveColor(tint: Color?): Color? = when (val resolvedElement = element) {
        is PatternElement.Dot -> tint?.let { blendColor(resolvedElement.color, it) }
        is PatternElement.Shape -> tint?.let { blendColor(resolvedElement.color, it) }
        else -> tint
    }

    fun resolveColorFilter(tint: Color?): ColorFilter? = when (val resolvedElement = element) {
        is PatternElement.Sprite -> (tint ?: resolvedElement.tint)?.let(ColorFilter::tint)
        else -> null
    }

    fun preparePass(tint: Color?, alpha: Float): PreparedElementPass = PreparedElementPass(
        color = resolveColor(tint),
        colorFilter = resolveColorFilter(tint),
        alpha = alpha,
    )
}

private sealed interface PreparedPatternInteraction {
    data object None : PreparedPatternInteraction

    data class UniformOffset(
        val maxOffsetPx: Float,
        val intensity: Float,
    ) : PreparedPatternInteraction

    data class DistanceBasedOffset(
        val maxOffsetPx: Float,
        val intensity: Float,
        val radiusPx: Float,
        val falloff: Falloff,
    ) : PreparedPatternInteraction
}

private sealed interface PreparedPatternDepth {
    data object None : PreparedPatternDepth

    data class Parallax(
        val maxOffsetPx: Float,
        val intensity: Float,
    ) : PreparedPatternDepth
}

private sealed interface PreparedPatternBlur {
    data object None : PreparedPatternBlur

    data class Soft(
        val alphaMultiplier: Float,
        val passes: Int,
        val scales: FloatArray,
    ) : PreparedPatternBlur
}

private sealed interface PreparedPatternShadow {
    data object None : PreparedPatternShadow

    data class Drop(
        val color: Color,
        val blurRadiusPx: Float,
        val baseOffset: Offset,
        val height: Float,
        val useInteractionAsLight: Boolean,
        val passCount: Int,
        val scales: FloatArray,
    ) : PreparedPatternShadow
}

private sealed interface PreparedPatternRotation {
    data object None : PreparedPatternRotation

    data class Uniform(
        val degreesPerSecond: Float,
        val directionSign: Float,
    ) : PreparedPatternRotation

    data class Async(
        val speeds: FloatArray,
        val directionSign: Float,
    ) : PreparedPatternRotation

    data class Distributed(
        val speeds: FloatArray,
        val directionSign: Float,
    ) : PreparedPatternRotation

    data class Follow(
        val wobbleAmplitudes: FloatArray,
        val wobbleSpeeds: FloatArray,
        val phases: FloatArray,
    ) : PreparedPatternRotation

    data class Directed(
        val defaultPoint: OffsetFraction,
        val useInteractionPoint: Boolean,
        val angleOffsetDegrees: Float,
    ) : PreparedPatternRotation
}

private data class PatternRenderBudget(
    val shadowPassMultiplier: Float,
    val blurPassMultiplier: Float,
)

private val RotationDirection.sign: Float
    get() = if (this == RotationDirection.Clockwise) 1f else -1f

private fun blendColor(base: Color, tint: Color): Color = lerp(base, tint, 0.68f)

private fun androidx.compose.ui.unit.DpSize.toPxSize(density: Density): Size = with(density) {
    Size(width.toPx(), height.toPx())
}

private fun Offset.normalize(): Offset {
    val distance = getDistance()
    return if (distance <= 0.0001f) Offset.Zero else Offset(x / distance, y / distance)
}

private operator fun Offset.times(value: Float): Offset = Offset(x * value, y * value)

private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + ((stop - start) * fraction)

private fun Float.scaleKey(): Int = toRawBits()

private const val MinimumVisibleAlpha = 0.01f
