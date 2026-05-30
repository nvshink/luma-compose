package dev.luma.visuals.defaults

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.luma.visuals.glow.GlowFalloff
import dev.luma.visuals.glow.GlowShape
import dev.luma.visuals.glow.GlowStyle

/** Preset styles for fast glow prototyping. */
object GlowDefaults {
    /** Tween-based follow motion with quick acceleration and soft deceleration. */
    fun easedFollowAnimation(durationMillis: Int = 180): FiniteAnimationSpec<dev.luma.visuals.model.OffsetFraction> = tween(
        durationMillis = durationMillis,
        easing = FastOutSlowInEasing,
    )

    /** Returns a soft circular blue glow. */
    fun softRadial(): GlowStyle = GlowStyle(
        color = Color(0xFF5B8CFF),
        alpha = 0.36f,
        blurRadius = 120.dp,
        size = DpSize(220.dp, 220.dp),
        shape = GlowShape.Circle,
        falloff = GlowFalloff.Soft,
    )

    /** Returns a brighter elongated pink glow suited for accent lighting. */
    fun strongEllipse(): GlowStyle = GlowStyle(
        color = Color(0xFFFF4D8D),
        alpha = 0.48f,
        blurRadius = 110.dp,
        size = DpSize(280.dp, 180.dp),
        shape = GlowShape.Ellipse,
        falloff = GlowFalloff.Focused,
    )

    /** Returns a large ambient rounded glow for broad background washes. */
    fun ambientBlob(): GlowStyle = GlowStyle(
        color = Color(0xFF7CF4C9),
        alpha = 0.26f,
        blurRadius = 150.dp,
        size = DpSize(260.dp, 220.dp),
        shape = GlowShape.RoundedRect(cornerRadius = 42.dp),
        falloff = GlowFalloff.Soft,
    )
}
