# luma-compose

`luma-compose` is a Kotlin Multiplatform multi-module library with reusable visual effects for Jetpack Compose.
The shared API lives in common source sets and currently targets Android, Desktop, iOS, and JS.

Main public composables:

- `PatternBackground` for decorative patterned backgrounds
- `FollowGlow` for a soft glow that follows a target point

The API stays app-agnostic: no product-specific models like compass, tilt, or level are built into the library.

## Import

Use the published artifact:

```kotlin
dependencies {
    implementation("io.github.nvshink:luma-compose:0.1.0")
}
```

For Kotlin Multiplatform consumers, use the shared coordinate in `commonMain`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.nvshink:luma-compose:0.1.0")
        }
    }
}
```

Published target-specific artifacts:

- `io.github.nvshink:luma-compose` - KMP root publication
- `io.github.nvshink:luma-compose-android` - Android artifact
- `io.github.nvshink:luma-compose-desktop` - Desktop JVM artifact
- `io.github.nvshink:luma-compose-js` - JS artifact
- `io.github.nvshink:luma-compose-iosx64` - iOS X64 artifact
- `io.github.nvshink:luma-compose-iosarm64` - iOS Arm64 artifact
- `io.github.nvshink:luma-compose-iossimulatorarm64` - iOS Simulator Arm64 artifact

Module layout:

- `:visuals` KMP library module with shared public API and presets
- `:android-demo` Android demo app with catalog screens and live controls
- `:desktop-demo` Compose Desktop demo app for trying the same effects on JVM desktop

## Quick Start

```kotlin
val patternStyle = PatternDefaults.softDots()
val glowStyle = GlowDefaults.softRadial()

Box(Modifier.fillMaxSize()) {
    PatternBackground(
        state = PatternState(
            interactionPoint = OffsetFraction(0.65f, 0.3f),
            isAnimated = true,
        ),
        style = patternStyle,
        modifier = Modifier.fillMaxSize(),
    )

    FollowGlow(
        state = GlowState(
            center = OffsetFraction(0.65f, 0.3f),
            isAnimated = true,
        ),
        style = glowStyle,
        modifier = Modifier.fillMaxSize(),
    )
}
```

## Features

### PatternBackground

`PatternBackground(state, style, modifier)` draws a reusable background from dots, shapes, sprites, or custom draw content.

Main settings:

- `PatternElement` controls what is rendered: `Dot`, `Shape`, `Sprite`, `Custom`
- `PatternLayout` controls placement: `Random`, `Uneven`, `Grid`, `Radial`, `Custom`
- `PatternRotation` controls motion: `None`, `Uniform`, `Async`, `Distributed`, `Follow`, `Directed`
- `PatternInteraction` controls point-based response: `UniformOffset`, `DistanceBasedOffset`
- `PatternShadow` configures shadow and light direction
- `PatternBlur` adds a lightweight soft blur layer
- `PatternDepth` adds parallax-like offset
- `PatternStyle` groups all visual parameters into one immutable style
- `PatternState` holds runtime inputs like animation and interaction point

Example:

```kotlin
val style = PatternStyle(
    element = PatternElement.Shape(
        size = DpSize(18.dp, 18.dp),
        shape = RoundedCornerShape(50),
        color = Color(0xFF7CF4C9),
    ),
    layout = PatternLayout.Grid(
        spacing = 32.dp,
        rowOffset = 16.dp,
        density = 1.2f,
    ),
    shadow = PatternShadow.Drop(
        blurRadius = 18.dp,
        useInteractionAsLight = true,
    ),
    blur = PatternBlur.Soft(radius = 10.dp),
    rotation = PatternRotation.Distributed(
        baseDegreesPerSecond = 12f,
        variation = 8f,
        seed = 42L,
    ),
    interaction = PatternInteraction.DistanceBasedOffset(
        point = OffsetFraction.Center,
        maxOffset = 18.dp,
        intensity = 0.8f,
        radius = 180.dp,
        falloff = Falloff.Smooth,
    ),
    elementTint = Color(0xFFB8FFE9),
    shadowTint = Color(0xFF1D6B57),
)
```

Ready presets:

```kotlin
val dots = PatternDefaults.softDots()
val stripes = PatternDefaults.ferriteStripes()
val sprites = PatternDefaults.floatingSprites()
val field = PatternDefaults.magneticNorthField()
```

Notes:

- `PatternDefaults` read colors from `MaterialTheme.colorScheme`
- `PatternState.isAnimated` enables internal time-driven animation
- `PatternState.animationTimeMillis` lets you drive animation externally
- `PatternState.interactionPoint` can be mapped to drag, cursor, sensor, or app state
- randomized models use `seed` for reproducibility

Custom sprite example:

```kotlin
val sprite = PatternElement.Sprite(
    size = DpSize(24.dp, 24.dp),
    painter = rememberVectorPainter(Icons.Rounded.Star),
    tint = Color(0xFFFFD166),
)
```

Custom layout example:

```kotlin
val waveLayout = PatternLayout.Custom(
    provider = PatternPositionProvider { scope ->
        List(20) { index ->
            val x = index / 19f
            OffsetFraction(x = x, y = 0.5f)
        }
    },
)
```

### FollowGlow

`FollowGlow(state, style, modifier)` draws a soft light layer tied to a target point.

Main settings:

- `GlowShape` controls silhouette: `Circle`, `Ellipse`, `RoundedRect`, `Custom`
- `GlowFalloff` controls gradient concentration: `Soft`, `Radial`, `Focused`
- `GlowTrailing` adds a short light trail
- `GlowStyle` groups color, alpha, size, blur, shape, and trail
- `GlowState` holds runtime inputs like center point, intensity, and follow animation

Example:

```kotlin
val glowStyle = GlowStyle(
    color = Color(0xFF5B8CFF),
    alpha = 0.36f,
    blurRadius = 120.dp,
    size = DpSize(220.dp, 220.dp),
    shape = GlowShape.Circle,
    trailing = GlowTrailing(steps = 3, alphaDecay = 0.08f),
)
```

Ready presets:

```kotlin
val softGlow = GlowDefaults.softRadial()
val ellipseGlow = GlowDefaults.strongEllipse()
val blobGlow = GlowDefaults.ambientBlob()
```

Notes:

- `GlowState.center` is the target position in fractional coordinates
- `GlowState.isAnimated` enables follow smoothing
- `GlowState.intensity` scales the visual output without rebuilding the style
- custom shapes can be drawn through `GlowShape.Custom`

## Performance

- layout positions are cached with `drawWithCache`
- keep `density`, `blurRadius`, and sprite size reasonable on large surfaces
- prefer fewer large glows over many overlapping glows
- `PatternBlur.Soft` is a lightweight approximation, not a full Gaussian blur pipeline
- custom draw elements should avoid heavy per-frame allocations

## Android Demo

The `:android-demo` module is a small catalog app for trying the library in isolation.
To use it, clone this repository and run the Android demo app from the local project.

Screens:

- `Pattern Background`
- `Glow`
- `Combined Demo`

What it includes:

- interactive controls for layouts, elements, animation, and interaction
- live switching between pattern and glow presets
- drag-driven target point previews
- a combined scene to test both effects together

Run the Android demo:

```bash
./gradlew :android-demo:installDebug
```

## Desktop Demo

The `:desktop-demo` module is a Compose Desktop app for previewing the library on desktop with the same visual primitives exposed by the shared KMP module.

Run the desktop demo:

```bash
./gradlew :desktop-demo:run
```
