package dev.luma.visuals.glow

import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import dev.luma.visuals.model.OffsetFraction
import kotlin.math.pow

/**
 * Draws a point-following glow that can be used as a background or overlay layer.
 *
 * @param state Runtime position and intensity inputs for the glow.
 * @param style Visual configuration that controls shape, color, blur and animation.
 * @param modifier Modifier applied to the drawing container.
 */
@Composable
fun FollowGlow(
    state: GlowState,
    style: GlowStyle,
    modifier: Modifier = Modifier,
) {
    val latestState = rememberUpdatedState(state)
    val animatedCenter = animateValueAsState(
        targetValue = when {
            state.currentCenter != null -> state.currentCenter
            else -> state.center
        },
        typeConverter = OffsetFractionVectorConverter,
        animationSpec = if (state.isAnimated && state.currentCenter == null) {
            style.animationSpec
        } else {
            androidx.compose.animation.core.snap()
        },
        label = "glow-follow-center",
    )

    Spacer(
        modifier = modifier.drawWithCache {
            val glowSize = style.size.toPxSize(this)
            val blurRadiusPx = style.blurRadius.toPx()
            val glowOffsetPx = Offset(style.offset.x.toPx(), style.offset.y.toPx())
            val glowPadding = glowContainerPadding(style.shape, glowSize, blurRadiusPx)
            val expandedSize = Size(
                width = glowSize.width + glowPadding.x * 2f,
                height = glowSize.height + glowPadding.y * 2f,
            )
            val cornerRadius = when (val shape = style.shape) {
                is GlowShape.RoundedRect -> CornerRadius(shape.cornerRadius.toPx(), shape.cornerRadius.toPx())
                else -> CornerRadius.Zero
            }
            onDrawBehind {
                val currentState = latestState.value
                val resolvedCenter = when {
                    currentState.currentCenter != null -> currentState.currentCenter
                    currentState.isAnimated -> animatedCenter.value
                    else -> currentState.center
                }
                drawGlowCopies(
                    centerFraction = resolvedCenter,
                    glowOffsetPx = glowOffsetPx,
                    glowSize = glowSize,
                    expandedSize = expandedSize,
                    glowPadding = glowPadding,
                    blurRadiusPx = blurRadiusPx,
                    cornerRadius = cornerRadius,
                    state = currentState,
                    style = style,
                )
            }
        },
    )
}

private fun DrawScope.drawGlowCopies(
    centerFraction: OffsetFraction,
    glowOffsetPx: Offset,
    glowSize: Size,
    expandedSize: Size,
    glowPadding: Offset,
    blurRadiusPx: Float,
    cornerRadius: CornerRadius,
    state: GlowState,
    style: GlowStyle,
) {
    val currentCenter = centerFraction.toOffset(size) + glowOffsetPx
    val alpha = style.alpha * state.intensity.coerceIn(0f, 1.5f)
    drawGlow(
        center = currentCenter,
        glowSize = glowSize,
        expandedSize = expandedSize,
        glowPadding = glowPadding,
        blurRadiusPx = blurRadiusPx,
        cornerRadius = cornerRadius,
        style = style,
        alpha = alpha,
    )
}

private fun DrawScope.drawGlow(
    center: Offset,
    glowSize: Size,
    expandedSize: Size,
    glowPadding: Offset,
    blurRadiusPx: Float,
    cornerRadius: CornerRadius,
    style: GlowStyle,
    alpha: Float,
) {
    if (alpha <= MinimumVisibleAlpha) return
    withTransform({
        translate(left = center.x - expandedSize.width / 2f, top = center.y - expandedSize.height / 2f)
    }) {
        drawSoftShapeGlow(
            shape = style.shape,
            glowSize = glowSize,
            expandedSize = expandedSize,
            glowPadding = glowPadding,
            blurRadiusPx = blurRadiusPx,
            color = style.color,
            falloff = style.falloff,
            cornerRadius = cornerRadius,
            alpha = alpha,
            blendMode = style.blendMode,
        )
    }
}

private fun DrawScope.drawSoftShapeGlow(
    shape: GlowShape,
    glowSize: Size,
    expandedSize: Size,
    glowPadding: Offset,
    blurRadiusPx: Float,
    color: Color,
    falloff: GlowFalloff,
    cornerRadius: CornerRadius,
    alpha: Float,
    blendMode: androidx.compose.ui.graphics.BlendMode,
) {
    val normalizedAlpha = alpha.coerceIn(0f, 1f)
    val passCount = resolveGlowPassCount(blurRadiusPx, glowSize, glowPadding)
    val baseTopLeft = glowPadding

    repeat(passCount) { index ->
        val progress = index / (passCount - 1f).coerceAtLeast(1f)
        val eased = resolveGlowExpansion(progress, falloff)
        val expansion = Offset(
            x = glowPadding.x * eased,
            y = glowPadding.y * eased,
        )
        val passTopLeft = baseTopLeft - expansion
        val passSize = Size(
            width = glowSize.width + expansion.x * 2f,
            height = glowSize.height + expansion.y * 2f,
        )
        val passAlpha = normalizedAlpha * resolveGlowPassAlpha(progress, passCount, falloff)
        if (passAlpha <= MinimumVisibleAlpha) return@repeat

        drawGlowShape(
            shape = shape,
            brush = SolidColor(color),
            topLeft = passTopLeft,
            size = passSize,
            cornerRadius = cornerRadius,
            alpha = passAlpha,
            blurRadiusPx = blurRadiusPx,
            blendMode = blendMode,
        )
    }
}

private fun DrawScope.drawGlowShape(
    shape: GlowShape,
    brush: Brush,
    topLeft: Offset,
    size: Size,
    cornerRadius: CornerRadius,
    alpha: Float,
    blurRadiusPx: Float,
    blendMode: androidx.compose.ui.graphics.BlendMode,
) {
    when (shape) {
        GlowShape.Circle -> drawCircle(
            brush = brush,
            radius = size.minDimension / 2f,
            center = Offset(topLeft.x + size.width / 2f, topLeft.y + size.height / 2f),
            alpha = alpha,
            blendMode = blendMode,
        )

        GlowShape.Ellipse -> drawOval(
            brush = brush,
            topLeft = topLeft,
            size = size,
            alpha = alpha,
            blendMode = blendMode,
        )

        is GlowShape.RoundedRect -> drawRoundRect(
            brush = brush,
            topLeft = topLeft,
            size = size,
            cornerRadius = cornerRadius,
            alpha = alpha,
            blendMode = blendMode,
        )

        is GlowShape.Custom -> shape.draw.invoke(
            this,
            GlowScope(
                center = Offset(topLeft.x + size.width / 2f, topLeft.y + size.height / 2f),
                size = size,
                alpha = alpha,
                blurRadiusPx = blurRadiusPx,
                brush = brush,
                blendMode = blendMode,
            ),
        )
    }
}

private fun DpSize.toPxSize(density: Density): Size = with(density) {
    Size(width.toPx(), height.toPx())
}

private fun glowContainerPadding(shape: GlowShape, glowSize: Size, blurRadiusPx: Float): Offset {
    val aspect = if (glowSize.height > 0f) glowSize.width / glowSize.height else 1f
    val shapeBoost = when (shape) {
        GlowShape.Circle -> 1f
        GlowShape.Ellipse -> 1.12f
        is GlowShape.RoundedRect -> 1.18f
        is GlowShape.Custom -> 1.24f
    }
    val xBias = kotlin.math.sqrt(aspect.coerceAtLeast(0.4f))
    val yBias = kotlin.math.sqrt((1f / aspect).coerceAtLeast(0.4f))
    return Offset(
        x = blurRadiusPx * shapeBoost * xBias,
        y = blurRadiusPx * shapeBoost * yBias,
    )
}

private fun resolveGlowPassCount(blurRadiusPx: Float, glowSize: Size, glowPadding: Offset): Int {
    val blurWeight = blurRadiusPx / 10f
    val sizeWeight = glowSize.maxDimension / 110f
    val paddingWeight = glowPadding.getDistance() / 90f
    return (blurWeight + sizeWeight + paddingWeight).toInt().coerceIn(14, 28)
}

private fun resolveGlowExpansion(progress: Float, falloff: GlowFalloff): Float = when (falloff) {
    GlowFalloff.Radial -> progress
    GlowFalloff.Soft -> progress.pow(0.82f)
    GlowFalloff.Focused -> progress.pow(1.18f)
}

private fun resolveGlowPassAlpha(progress: Float, passCount: Int, falloff: GlowFalloff): Float {
    val sigma = when (falloff) {
        GlowFalloff.Radial -> 0.78f
        GlowFalloff.Soft -> 0.96f
        GlowFalloff.Focused -> 0.58f
    }
    val gaussianLike = kotlin.math.exp(-(progress * progress) / (2f * sigma * sigma))
    return (gaussianLike / passCount) * 2.6f
}

private val OffsetFractionVectorConverter = TwoWayConverter<OffsetFraction, AnimationVector2D>(
    convertToVector = { AnimationVector2D(it.x, it.y) },
    convertFromVector = { OffsetFraction(it.v1, it.v2) },
)

private fun Float.pow(power: Float): Float = toDouble().pow(power.toDouble()).toFloat()

private const val MinimumVisibleAlpha = 0.01f
