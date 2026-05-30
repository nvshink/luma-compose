package dev.luma.visuals.pattern.animation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.luma.visuals.model.Falloff
import dev.luma.visuals.model.OffsetFraction

/** Direction used by rotation-based pattern animation. */
enum class RotationDirection {
    Clockwise,
    CounterClockwise,
}

/** Rotation behavior for pattern elements. */
sealed interface PatternRotation {
    data object None : PatternRotation

    /**
     * Applies the same continuous rotation to every pattern element.
     *
     * @property degreesPerSecond Rotation speed shared by all elements.
     * @property direction Rotation direction applied to the shared speed.
     */
    @Immutable
    data class Uniform(
        val degreesPerSecond: Float,
        val direction: RotationDirection = RotationDirection.Clockwise,
    ) : PatternRotation

    /**
     * Applies the same direction with a shared base speed and seeded per-element speed multiplier.
     *
     * @property baseDegreesPerSecond Baseline rotation speed for all elements.
     * @property speedMultiplierVariation Maximum random multiplier added to each element speed.
     * @property seed Seed used to keep per-element speed variance stable across frames.
     * @property direction Rotation direction applied to the resolved speed.
     */
    @Immutable
    data class Async(
        val baseDegreesPerSecond: Float,
        val speedMultiplierVariation: Float,
        val seed: Long,
        val direction: RotationDirection = RotationDirection.Clockwise,
    ) : PatternRotation

    /**
     * Applies seeded per-element rotation speed variance around a shared base speed.
     *
     * @property baseDegreesPerSecond Baseline rotation speed for all elements.
     * @property variation Maximum random variation added per element.
     * @property seed Seed used to keep per-element variation stable across frames.
     * @property direction Base rotation direction applied to the resolved speed.
     */
    @Immutable
    data class Distributed(
        val baseDegreesPerSecond: Float,
        val variation: Float,
        val seed: Long,
        val direction: RotationDirection = RotationDirection.Clockwise,
    ) : PatternRotation

    /**
     * Applies a seeded wobble that can visually follow interaction-driven motion.
     *
     * @property wobbleAmplitudeDegrees Maximum angular wobble amplitude.
     * @property wobbleSpeedMultiplier Oscillation speed multiplier expressed in cycles per second.
     * @property variation Additional seeded variation applied to amplitude and speed.
     * @property seed Seed used to keep wobble variation stable across elements.
     */
    @Immutable
    data class Follow(
        val wobbleAmplitudeDegrees: Float = 0f,
        val wobbleSpeedMultiplier: Float = 0f,
        val variation: Float = 0f,
        val seed: Long = 0L,
    ) : PatternRotation

    /**
     * Rotates every element so it points toward one target position.
     *
     * @property point Default target point used when runtime state does not provide one.
     * @property useInteractionPoint Uses the runtime interaction point as the target when available.
     * @property angleOffsetDegrees Extra angle applied after the target direction is resolved.
     */
    @Immutable
    data class Directed(
        val point: OffsetFraction = OffsetFraction.Center,
        val useInteractionPoint: Boolean = true,
        val angleOffsetDegrees: Float = 0f,
    ) : PatternRotation
}

/** Pointer-like interaction that offsets pattern elements. */
sealed interface PatternInteraction {
    data object None : PatternInteraction

    /**
     * Offsets all elements in the same direction relative to an interaction point.
     *
     * @property point Default interaction point used when runtime state does not provide one.
     * @property maxOffset Maximum translation magnitude per element.
     * @property intensity Strength multiplier applied to the resulting offset.
     */
    @Immutable
    data class UniformOffset(
        val point: OffsetFraction = OffsetFraction.Center,
        val maxOffset: Dp = 12.dp,
        val intensity: Float = 1f,
    ) : PatternInteraction

    /**
     * Offsets elements away from an interaction point based on distance and falloff.
     *
     * @property point Default interaction point used when runtime state does not provide one.
     * @property maxOffset Maximum translation magnitude for fully influenced elements.
     * @property intensity Strength multiplier applied to the computed influence.
     * @property radius Radius within which the interaction contributes to element motion.
     * @property falloff Curve that maps distance to influence strength.
     */
    @Immutable
    data class DistanceBasedOffset(
        val point: OffsetFraction = OffsetFraction.Center,
        val maxOffset: Dp = 18.dp,
        val intensity: Float = 1f,
        val radius: Dp = 180.dp,
        val falloff: Falloff = Falloff.Smooth,
    ) : PatternInteraction
}
