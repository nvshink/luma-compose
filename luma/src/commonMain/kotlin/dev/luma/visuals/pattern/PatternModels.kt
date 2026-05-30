package dev.luma.visuals.pattern

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape as GraphicsShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.luma.visuals.model.OffsetFraction
import dev.luma.visuals.pattern.animation.PatternInteraction
import dev.luma.visuals.pattern.animation.PatternRotation
import dev.luma.visuals.pattern.layout.PatternLayout

/** Base element rendered by [PatternBackground]. */
sealed interface PatternElement {
    /** Base unscaled size of the rendered element. */
    val size: DpSize

    /**
     * Circular element rendered as a filled dot.
     *
     * @property radius Dot radius.
     * @property color Fill color used for the dot.
     */
    @Immutable
    data class Dot(
        val radius: Dp,
        val color: Color,
    ) : PatternElement {
        override val size: DpSize = DpSize(radius * 2, radius * 2)
    }

    /**
     * Shape-backed element rendered from a Compose [GraphicsShape].
     *
     * @property size Unscaled size of the shape bounds.
     * @property shape Shape outline used for rendering.
     * @property color Fill color used for the shape.
     */
    @Immutable
    data class Shape(
        override val size: DpSize,
        val shape: GraphicsShape,
        val color: Color,
    ) : PatternElement

    /**
     * Painter-backed element rendered from a supplied sprite.
     *
     * @property size Unscaled size of the sprite bounds.
     * @property painter Painter used to draw the sprite.
     * @property tint Optional tint applied to the painter output.
     */
    @Immutable
    data class Sprite(
        override val size: DpSize,
        val painter: Painter,
        val tint: Color? = null,
    ) : PatternElement

    /**
     * Fully custom element renderer.
     *
     * @property size Unscaled size available to the drawing lambda.
     * @property draw Drawing lambda invoked for each rendered element.
     */
    @Immutable
    data class Custom(
        override val size: DpSize,
        val draw: DrawScope.(PatternElementScope) -> Unit,
    ) : PatternElement
}

/**
 * Render-time metadata exposed to custom pattern drawing lambdas.
 *
 * @property nodeIndex Zero-based index of the resolved element position.
 * @property size Resolved draw size after scale has been applied.
 * @property alpha Effective alpha for the current draw pass.
 * @property rotationDegrees Effective rotation for the current draw pass.
 * @property interactionAmount Normalized interaction influence affecting the element.
 * @property isShadowPass Indicates whether the current draw pass is rendering a shadow.
 * @property tint Optional active tint propagated from the current [PatternStyle] draw pass.
 */
@Immutable
data class PatternElementScope(
    val nodeIndex: Int,
    val size: Size,
    val alpha: Float,
    val rotationDegrees: Float,
    val interactionAmount: Float,
    val isShadowPass: Boolean,
    val tint: Color?,
)

/** Shadow model applied before the main element pass. */
sealed interface PatternShadow {
    data object None : PatternShadow

    /**
     * Drop shadow rendered as an offset copy of the element.
     *
     * @property color Shadow tint and base alpha.
     * @property blurRadius Radius used to extend the interaction-aware shadow offset.
     * @property offset Base shadow translation.
     * @property height Virtual object height used as a multiplier for the cast shadow length.
     * @property spread Scale multiplier applied to the shadow pass.
     * @property useInteractionAsLight Enables shadow displacement away from the interaction point.
     */
    @Immutable
    data class Drop(
        val color: Color = Color.Black.copy(alpha = 0.22f),
        val blurRadius: Dp = 16.dp,
        val offset: DpOffset = DpOffset(0.dp, 8.dp),
        val height: Float = 1f,
        val spread: Float = 1.12f,
        val useInteractionAsLight: Boolean = false,
    ) : PatternShadow
}

/** Lightweight blur approximation used for background elements. */
sealed interface PatternBlur {
    data object None : PatternBlur

    /**
     * Simulates blur by drawing extra translucent scaled passes.
     *
     * @property radius Target blur radius used to derive pass scaling.
     * @property passes Number of translucent blur passes to render.
     * @property alphaMultiplier Alpha multiplier applied to each blur pass.
     */
    @Immutable
    data class Soft(
        val radius: Dp = 12.dp,
        val passes: Int = 2,
        val alphaMultiplier: Float = 0.18f,
    ) : PatternBlur
}

/** Optional depth shift that reacts to the active interaction point. */
sealed interface PatternDepth {
    data object None : PatternDepth

    /**
     * Applies a parallax offset scaled by distance from the container center.
     *
     * @property maxOffset Maximum positional shift applied at full influence.
     * @property intensity Strength multiplier applied to the parallax effect.
     */
    @Immutable
    data class Parallax(
        val maxOffset: Dp = 12.dp,
        val intensity: Float = 0.35f,
    ) : PatternDepth
}

/**
 * Immutable style bundle for [PatternBackground].
 *
 * @property element Element definition rendered at each resolved layout position.
 * @property layout Layout strategy used to generate element positions.
 * @property shadow Optional shadow pass rendered before the main element.
 * @property blur Optional blur approximation rendered before the main element.
 * @property rotation Rotation behavior applied to each element.
 * @property interaction Interaction model that offsets elements at runtime.
 * @property alpha Base alpha for the main element pass.
 * @property scale Base scale multiplier for the element.
 * @property tint Optional legacy tint blended with the element's own color when [elementTint] is unset.
 * @property elementTint Optional tint blended with the element's own color for the main and blur passes.
 * @property shadowTint Optional tint used for the shadow pass. Falls back to [PatternShadow.Drop.color].
 * @property depth Optional depth/parallax behavior driven by interaction.
 */
@Immutable
data class PatternStyle(
    val element: PatternElement,
    val layout: PatternLayout,
    val shadow: PatternShadow = PatternShadow.None,
    val blur: PatternBlur = PatternBlur.None,
    val rotation: PatternRotation = PatternRotation.None,
    val interaction: PatternInteraction = PatternInteraction.None,
    val alpha: Float = 1f,
    val scale: Float = 1f,
    val tint: Color? = null,
    val elementTint: Color? = null,
    val shadowTint: Color? = null,
    val depth: PatternDepth = PatternDepth.None,
)

/**
 * Mutable runtime inputs for [PatternBackground].
 *
 * @property animationTimeMillis Explicit animation clock in milliseconds. When zero and [isAnimated] is true,
 * a frame clock is used internally.
 * @property interactionPoint Optional runtime interaction point in fractional coordinates.
 * @property baseRotationDegrees Constant rotation added on top of animated rotation.
 * @property isAnimated Enables internal frame-driven animation when no explicit clock is supplied.
 */
@Immutable
data class PatternState(
    val animationTimeMillis: Long = 0L,
    val interactionPoint: OffsetFraction? = null,
    val baseRotationDegrees: Float = 0f,
    val isAnimated: Boolean = false,
)
