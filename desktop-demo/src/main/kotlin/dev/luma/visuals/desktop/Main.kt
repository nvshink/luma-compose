package dev.luma.visuals.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.luma.visuals.defaults.GlowDefaults
import dev.luma.visuals.defaults.PatternDefaults
import dev.luma.visuals.glow.FollowGlow
import dev.luma.visuals.glow.GlowFalloff
import dev.luma.visuals.glow.GlowShape
import dev.luma.visuals.glow.GlowState
import dev.luma.visuals.glow.GlowStyle
import dev.luma.visuals.model.Falloff
import dev.luma.visuals.model.OffsetFraction
import dev.luma.visuals.pattern.PatternBackground
import dev.luma.visuals.pattern.PatternElement
import dev.luma.visuals.pattern.PatternBlur
import dev.luma.visuals.pattern.PatternDepth
import dev.luma.visuals.pattern.PatternShadow
import dev.luma.visuals.pattern.PatternState
import dev.luma.visuals.pattern.PatternStyle
import dev.luma.visuals.pattern.animation.PatternInteraction
import dev.luma.visuals.pattern.animation.PatternRotation
import dev.luma.visuals.pattern.layout.PatternLayout
import dev.luma.visuals.pattern.layout.PatternLayoutScope
import dev.luma.visuals.pattern.layout.PatternPositionProvider
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class DesktopScreen(val label: String) {
    Pattern("Pattern Background"),
    Glow("Glow"),
}

private enum class GlowShapeOption(val label: String) {
    Circle("Circle"),
    Ellipse("Ellipse"),
    Rounded("Rounded"),
    Custom("Custom"),
}

private enum class InteractionMode(val label: String) {
    Drag("Drag"),
    Cursor("Cursor"),
}

private enum class PatternInteractionOption(val label: String) {
    None("None"),
    Uniform("Uniform"),
    Distance("Distance"),
}

private enum class LayoutOption(val label: String) {
    Random("Random"),
    Uneven("Uneven"),
    Grid("Row"),
    Radial("Radial"),
    Custom("Custom"),
}

private enum class ElementOption(val label: String) {
    Dot("Dot"),
    Shape("Shape"),
    Sprite("Sprite"),
    Custom("Custom"),
}

private enum class RotationOption(val label: String) {
    None("None"),
    Uniform("Sync"),
    Async("Async"),
    Directed("Directed"),
    Distributed("Distributed"),
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Luma Compose Desktop Demo",
    ) {
        DesktopDemoApp()
    }
}

@Composable
private fun DesktopDemoApp() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF8AA8FF),
            secondary = Color(0xFF72E6C8),
            tertiary = Color(0xFFFF9AC2),
            background = Color(0xFF050816),
            surface = Color(0xFF0C1226),
            surfaceVariant = Color(0xFF121B35),
        ),
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
            DesktopCatalogScreen()
        }
    }
}

@Composable
private fun DesktopCatalogScreen() {
    var screen by remember { mutableStateOf(DesktopScreen.Pattern) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Luma Visuals",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "PatternBackground + FollowGlow as reusable Compose-first building blocks on Desktop.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            PrimaryTabRow(selectedTabIndex = screen.ordinal) {
                DesktopScreen.entries.forEach { tab ->
                    Tab(
                        selected = screen == tab,
                        onClick = { screen = tab },
                        text = { Text(tab.label) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            when (screen) {
                DesktopScreen.Pattern -> PatternDesktopScreen()
                DesktopScreen.Glow -> GlowDesktopScreen()
            }
        }
    }
}

@Composable
private fun PatternDesktopScreen() {
    val colorScheme = MaterialTheme.colorScheme
    var layout by remember { mutableStateOf(LayoutOption.Random) }
    var element by remember { mutableStateOf(ElementOption.Dot) }
    var rotation by remember { mutableStateOf(RotationOption.Distributed) }
    var interaction by remember { mutableStateOf(PatternInteractionOption.Distance) }
    var mode by remember { mutableStateOf(InteractionMode.Cursor) }
    var animated by remember { mutableStateOf(true) }
    var shadowEnabled by remember { mutableStateOf(true) }
    var density by remember { mutableFloatStateOf(1.4f) }
    var spacing by remember { mutableFloatStateOf(36f) }
    var blur by remember { mutableFloatStateOf(12f) }
    var shadowHeight by remember { mutableFloatStateOf(1.2f) }
    var speed by remember { mutableFloatStateOf(14f) }
    var intensity by remember { mutableFloatStateOf(0.75f) }
    val spritePainter = rememberDemoSpritePainter()

    val layoutConfig = remember(layout, density, spacing) {
        when (layout) {
            LayoutOption.Random -> PatternLayout.Random(seed = 7L, density = density)
            LayoutOption.Uneven -> PatternLayout.Uneven(seed = 7L, density = density)
            LayoutOption.Grid -> PatternLayout.Grid(spacing = spacing.dp, rowOffset = (spacing / 2f).dp, density = density)
            LayoutOption.Radial -> PatternLayout.Radial(center = OffsetFraction.Center, ringSpacing = spacing.dp, density = density)
            LayoutOption.Custom -> PatternLayout.Custom(CustomWaveProvider)
        }
    }
    val elementConfig = remember(element, spritePainter, colorScheme) {
        when (element) {
            ElementOption.Dot -> PatternElement.Dot(radius = 5.dp, color = colorScheme.primaryContainer)
            ElementOption.Shape -> PatternElement.Shape(
                size = DpSize(18.dp, 18.dp),
                shape = RoundedCornerShape(35),
                color = colorScheme.secondaryContainer,
            )

            ElementOption.Sprite -> PatternElement.Sprite(
                size = DpSize(24.dp, 24.dp),
                painter = spritePainter,
                tint = colorScheme.tertiaryContainer,
            )

            ElementOption.Custom -> PatternElement.Custom(size = DpSize(22.dp, 22.dp)) { scope ->
                val path = Path().apply {
                    moveTo(scope.size.width / 2f, 0f)
                    lineTo(scope.size.width, scope.size.height / 2f)
                    lineTo(scope.size.width / 2f, scope.size.height)
                    lineTo(0f, scope.size.height / 2f)
                    close()
                }
                drawPath(path = path, color = scope.tint ?: colorScheme.tertiary, alpha = scope.alpha)
            }
        }
    }
    val rotationConfig = remember(rotation, speed) {
        when (rotation) {
            RotationOption.None -> PatternRotation.None
            RotationOption.Uniform -> PatternRotation.Uniform(degreesPerSecond = speed)
            RotationOption.Async -> PatternRotation.Async(
                baseDegreesPerSecond = speed,
                speedMultiplierVariation = 0.85f,
                seed = 12L,
            )
            RotationOption.Directed -> PatternRotation.Directed(point = OffsetFraction.Center)
            RotationOption.Distributed -> PatternRotation.Distributed(baseDegreesPerSecond = speed, variation = speed * 0.75f, seed = 12L)
        }
    }

    val interactionConfig = remember(interaction, intensity) {
        when (interaction) {
            PatternInteractionOption.None -> PatternInteraction.None
            PatternInteractionOption.Uniform -> PatternInteraction.UniformOffset(maxOffset = 14.dp, intensity = intensity)
            PatternInteractionOption.Distance -> PatternInteraction.DistanceBasedOffset(
                maxOffset = 18.dp,
                intensity = intensity,
                radius = 180.dp,
                falloff = Falloff.Smooth,
            )
        }
    }
    val style = remember(
        elementConfig,
        layoutConfig,
        shadowEnabled,
        blur,
        shadowHeight,
        rotationConfig,
        interactionConfig,
        intensity,
        colorScheme,
        interaction,
    ) {
        PatternStyle(
            element = elementConfig,
            layout = layoutConfig,
            shadow = if (shadowEnabled) {
                PatternShadow.Drop(
                    color = colorScheme.primary.copy(alpha = 0.22f),
                    blurRadius = blur.dp,
                    height = shadowHeight,
                    useInteractionAsLight = interaction != PatternInteractionOption.None,
                )
            } else {
                PatternShadow.None
            },
            blur = if (blur > 0.5f) PatternBlur.Soft(radius = blur.dp, passes = 2) else PatternBlur.None,
            rotation = rotationConfig,
            interaction = interactionConfig,
            alpha = 0.92f,
            scale = 1f,
            depth = if (interaction != PatternInteractionOption.None) {
                PatternDepth.Parallax(maxOffset = 10.dp, intensity = intensity * 0.35f)
            } else {
                PatternDepth.None
            },
        )
    }
    val prompt = remember(layout, element, rotation, interaction, mode, animated, shadowEnabled, density, spacing, blur, shadowHeight, speed, intensity) {
        buildPatternPrompt(
            layout = layout,
            element = element,
            rotation = rotation,
            interaction = interaction,
            mode = mode,
            animated = animated,
            shadowEnabled = shadowEnabled,
            density = density,
            spacing = spacing,
            blur = blur,
            shadowHeight = shadowHeight,
            speed = speed,
            intensity = intensity,
        )
    }

    DemoScreenFrame(
        preview = {
            PatternPreview(style = style, animated = animated, mode = mode)
        },
    ) {
        Text("Layout", style = MaterialTheme.typography.titleMedium)
        EnumChips(LayoutOption.entries, layout, { layout = it }) { it.label }
        Text("Element", style = MaterialTheme.typography.titleMedium)
        EnumChips(ElementOption.entries, element, { element = it }) { it.label }
        Text("Shadow", style = MaterialTheme.typography.titleMedium)
        BooleanChips(selected = shadowEnabled, trueLabel = "Enabled", falseLabel = "Disabled", onSelected = { shadowEnabled = it })
        Text("Rotation", style = MaterialTheme.typography.titleMedium)
        EnumChips(RotationOption.entries, rotation, { rotation = it }) { it.label }
        Text("Interaction", style = MaterialTheme.typography.titleMedium)
        EnumChips(PatternInteractionOption.entries, interaction, { interaction = it }) { it.label }
        Text("Pointer Mode", style = MaterialTheme.typography.titleMedium)
        EnumChips(InteractionMode.entries, mode, { mode = it }) { it.label }
        BooleanChips(selected = animated, trueLabel = "Animated", falseLabel = "Static", onSelected = { animated = it })
        LabeledSlider("Density", density, 0.4f..2.6f) { density = it }
        LabeledSlider("Spacing", spacing, 20f..72f) { spacing = it }
        LabeledSlider("Blur", blur, 0f..28f) { blur = it }
        LabeledSlider("Shadow Height", shadowHeight, 0.4f..3f) { shadowHeight = it }
        LabeledSlider("Speed", speed, 0f..40f) { speed = it }
        LabeledSlider("Intensity", intensity, 0f..1.2f) { intensity = it }
        PromptBlock(prompt)
    }
}

@Composable
private fun GlowDesktopScreen() {
    val colorScheme = MaterialTheme.colorScheme
    var shape by remember { mutableStateOf(GlowShapeOption.Circle) }
    var follow by remember { mutableStateOf(true) }
    var mode by remember { mutableStateOf(InteractionMode.Drag) }
    var blur by remember { mutableFloatStateOf(110f) }
    var width by remember { mutableFloatStateOf(240f) }
    var height by remember { mutableFloatStateOf(240f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var alpha by remember { mutableFloatStateOf(0.52f) }
    var intensity by remember { mutableFloatStateOf(1f) }

    val glowShape = remember(shape) {
        when (shape) {
            GlowShapeOption.Circle -> GlowShape.Circle
            GlowShapeOption.Ellipse -> GlowShape.Ellipse
            GlowShapeOption.Rounded -> GlowShape.RoundedRect(cornerRadius = 40.dp)
            GlowShapeOption.Custom -> GlowShape.Custom { scope ->
                val path = Path().apply {
                    moveTo(scope.size.width * 0.5f, 0f)
                    cubicTo(
                        scope.size.width * 1.05f,
                        scope.size.height * 0.18f,
                        scope.size.width * 0.94f,
                        scope.size.height * 0.88f,
                        scope.size.width * 0.56f,
                        scope.size.height,
                    )
                    cubicTo(
                        scope.size.width * 0.12f,
                        scope.size.height * 0.92f,
                        -scope.size.width * 0.04f,
                        scope.size.height * 0.22f,
                        scope.size.width * 0.5f,
                        0f,
                    )
                }
                drawPath(path = path, brush = scope.brush, alpha = scope.alpha, blendMode = scope.blendMode)
            }
        }
    }
    val style = remember(glowShape, shape, alpha, blur, width, height, offsetX, offsetY, colorScheme.primary) {
        GlowStyle(
            color = colorScheme.primary,
            alpha = alpha,
            blurRadius = blur.dp,
            size = DpSize(width.dp, height.dp),
            offset = DpOffset(offsetX.dp, offsetY.dp),
            shape = glowShape,
            falloff = if (shape == GlowShapeOption.Circle) GlowFalloff.Soft else GlowFalloff.Radial,
        )
    }
    val prompt = remember(shape, follow, mode, blur, width, height, offsetX, offsetY, alpha, intensity) {
        buildGlowPrompt(
            shape = shape,
            follow = follow,
            mode = mode,
            blur = blur,
            width = width,
            height = height,
            offsetX = offsetX,
            offsetY = offsetY,
            alpha = alpha,
            intensity = intensity,
        )
    }

    DemoScreenFrame(
        preview = {
            GlowPreview(style = style, intensity = intensity, follow = follow, mode = mode)
        },
    ) {
        Text("Glow Shape", style = MaterialTheme.typography.titleMedium)
        EnumChips(GlowShapeOption.entries, shape, { shape = it }) { it.label }
        Text("Pointer Mode", style = MaterialTheme.typography.titleMedium)
        EnumChips(InteractionMode.entries, mode, { mode = it }) { it.label }
        BooleanChips(selected = follow, trueLabel = "Follow On", falseLabel = "Follow Off", onSelected = { follow = it })
        LabeledSlider("Blur", blur, 20f..180f) { blur = it }
        LabeledSlider("Width", width, 100f..320f) { width = it }
        LabeledSlider("Height", height, 100f..320f) { height = it }
        LabeledSlider("Offset X", offsetX, -160f..160f) { offsetX = it }
        LabeledSlider("Offset Y", offsetY, -160f..160f) { offsetY = it }
        LabeledSlider("Alpha", alpha, 0.08f..0.8f) { alpha = it }
        LabeledSlider("Intensity", intensity, 0f..1.4f) { intensity = it }
        PromptBlock(prompt)
    }
}

@Composable
private fun PatternPreview(
    style: PatternStyle,
    animated: Boolean,
    mode: InteractionMode,
) {
    var pointer by remember { mutableStateOf(OffsetFraction.Center) }
    val colorScheme = MaterialTheme.colorScheme
    val background = remember(colorScheme) {
        Brush.linearGradient(
            colors = listOf(
                colorScheme.surface,
                colorScheme.surfaceContainerHigh,
                colorScheme.surface,
            ),
        )
    }
    InteractiveStage(
        point = pointer,
        onPointChange = { pointer = it },
        mode = mode,
        modifier = Modifier.fillMaxSize(),
        showMarker = false,
    ) {
        Box(Modifier.fillMaxSize().background(background)) {
            PatternBackground(
                state = PatternState(isAnimated = animated, interactionPoint = pointer),
                style = style,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun GlowPreview(
    style: GlowStyle,
    intensity: Float,
    follow: Boolean,
    mode: InteractionMode,
) {
    val colorScheme = MaterialTheme.colorScheme
    var center by remember { mutableStateOf(OffsetFraction(0.4f, 0.45f)) }
    InteractiveStage(
        point = center,
        onPointChange = { center = it },
        mode = mode,
        modifier = Modifier.fillMaxSize(),
        showMarker = false,
    ) {
        Box(Modifier.fillMaxSize().background(colorScheme.surface)) {
            FollowGlow(
                state = GlowState(
                    center = center,
                    intensity = intensity,
                    isAnimated = follow,
                ),
                style = style,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun DemoScreenFrame(
    preview: @Composable () -> Unit,
    settings: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight(),
        ) {
            preview()
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            settings()
        }
    }
}

@Composable
private fun InteractiveStage(
    point: OffsetFraction,
    onPointChange: (OffsetFraction) -> Unit,
    mode: InteractionMode,
    modifier: Modifier = Modifier,
    showMarker: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val markerSize = 14.dp
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
                .pointerInput(containerSize, mode) {
                    when (mode) {
                        InteractionMode.Drag -> detectDragGestures(
                            onDragStart = { offset -> onPointChange(offset.toFraction(containerSize)) },
                            onDrag = { change, _ ->
                                onPointChange(change.position.toFraction(containerSize))
                                change.consume()
                            },
                        )

                        InteractionMode.Cursor -> awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val position = event.changes.firstOrNull()?.position ?: continue
                                onPointChange(position.toFraction(containerSize))
                            }
                        }
                    }
                }
                .padding(1.dp),
        ) {
            content()
            if (showMarker) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offsetForFraction(point, containerSize, markerSize.value)
                        .size(markerSize)
                        .border(2.dp, colorScheme.onSurface.copy(alpha = 0.92f), CircleShape)
                        .background(Color.Transparent, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun PromptBlock(prompt: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("AI Prompt", style = MaterialTheme.typography.titleMedium)
            SelectionContainer {
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun <T> EnumChips(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    label: (T) -> String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    FilterChip(
                        selected = option == selected,
                        onClick = { onSelected(option) },
                        label = { Text(label(option)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BooleanChips(
    selected: Boolean,
    trueLabel: String,
    falseLabel: String,
    onSelected: (Boolean) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected,
            onClick = { onSelected(true) },
            label = { Text(trueLabel) },
        )
        FilterChip(
            selected = !selected,
            onClick = { onSelected(false) },
            label = { Text(falseLabel) },
        )
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "$label: ${formatValue(value)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
        )
    }
}

private fun formatValue(value: Float): String = if (value >= 100f || value % 1f == 0f) {
    value.roundToInt().toString()
} else {
    String.format("%.2f", value)
}

private fun buildPatternPrompt(
    layout: LayoutOption,
    element: ElementOption,
    rotation: RotationOption,
    interaction: PatternInteractionOption,
    mode: InteractionMode,
    animated: Boolean,
    shadowEnabled: Boolean,
    density: Float,
    spacing: Float,
    blur: Float,
    shadowHeight: Float,
    speed: Float,
    intensity: Float,
): String = buildString {
    append("Use the Luma Compose library to configure a PatternBackground in Jetpack Compose for Desktop. ")
    append("Set the layout to ")
    append(
        when (layout) {
            LayoutOption.Random -> "PatternLayout.Random for a random scattered composition"
            LayoutOption.Uneven -> "PatternLayout.Uneven for an irregular clustered composition"
            LayoutOption.Grid -> "PatternLayout.Grid for a structured row-based layout"
            LayoutOption.Radial -> "PatternLayout.Radial for a center-weighted radial composition"
            LayoutOption.Custom -> "PatternLayout.Custom for a flowing wave-based custom layout"
        },
    )
    append(", and use ")
    append(
        when (element) {
            ElementOption.Dot -> "PatternElement.Dot"
            ElementOption.Shape -> "PatternElement.Shape"
            ElementOption.Sprite -> "PatternElement.Sprite"
            ElementOption.Custom -> "PatternElement.Custom"
        },
    )
    append(" for the rendered nodes. ")
    append(
        when (interaction) {
            PatternInteractionOption.None -> "Use PatternInteraction.None. "
            PatternInteractionOption.Uniform -> "Use PatternInteraction.UniformOffset for group motion. "
            PatternInteractionOption.Distance -> "Use PatternInteraction.DistanceBasedOffset for reactive depth. "
        },
    )
    append("Pointer mode should be ${mode.label.lowercase()}. ")
    append(if (animated) "Enable animation. " else "Keep animation static. ")
    append(
        when (rotation) {
            RotationOption.None -> "Use PatternRotation.None. "
            RotationOption.Uniform -> "Use PatternRotation.Uniform for synchronous rotational motion. "
            RotationOption.Async -> "Use PatternRotation.Async for shared direction with per-element speed variance. "
            RotationOption.Directed -> "Use PatternRotation.Directed so all elements aim at one target point. "
            RotationOption.Distributed -> "Use PatternRotation.Distributed for varied per-element rotation. "
        },
    )
    append(if (shadowEnabled) "Configure PatternShadow.Drop for soft depth. " else "Disable shadows with PatternShadow.None. ")
    append("Set density to ${formatValue(density)}, spacing to ${formatValue(spacing)} dp where applicable, blur radius to ${formatValue(blur)} dp, shadow height to ${formatValue(shadowHeight)}, rotation speed to ${formatValue(speed)}, and interaction intensity to ${formatValue(intensity)}. ")
    append("Return Compose code that uses PatternStyle and PatternState with these settings, not a new rendering system.")
}

private fun buildGlowPrompt(
    shape: GlowShapeOption,
    follow: Boolean,
    mode: InteractionMode,
    blur: Float,
    width: Float,
    height: Float,
    offsetX: Float,
    offsetY: Float,
    alpha: Float,
    intensity: Float,
): String = buildString {
    append("Use Luma Compose FollowGlow for desktop. ")
    append(
        when (shape) {
            GlowShapeOption.Circle -> "Set shape to GlowShape.Circle. "
            GlowShapeOption.Ellipse -> "Set shape to GlowShape.Ellipse. "
            GlowShapeOption.Rounded -> "Set shape to GlowShape.RoundedRect. "
            GlowShapeOption.Custom -> "Set shape to GlowShape.Custom with organic blob silhouette. "
        },
    )
    append("Pointer mode should be ${mode.label.lowercase()}. ")
    append(if (follow) "Enable follow animation. " else "Disable follow animation. ")
    append("Set blur radius to ${formatValue(blur)} dp, size to ${formatValue(width)} x ${formatValue(height)} dp, offset to ${formatValue(offsetX)} x ${formatValue(offsetY)} dp, alpha to ${formatValue(alpha)}, intensity to ${formatValue(intensity)}. ")
    append("Return Compose code using FollowGlow, GlowStyle, and GlowState.")
}

private fun Offset.toFraction(containerSize: IntSize): OffsetFraction {
    val width = containerSize.width.toFloat().coerceAtLeast(1f)
    val height = containerSize.height.toFloat().coerceAtLeast(1f)
    return OffsetFraction(
        x = (x / width).coerceIn(0f, 1f),
        y = (y / height).coerceIn(0f, 1f),
    )
}

private fun Modifier.offsetForFraction(
    point: OffsetFraction,
    containerSize: IntSize,
    markerSize: Float,
): Modifier {
    return offset {
        IntOffset(
            x = ((containerSize.width * point.x) - markerSize / 2f).roundToInt(),
            y = ((containerSize.height * point.y) - markerSize / 2f).roundToInt(),
        )
    }
}


private val CustomWaveProvider = PatternPositionProvider { scope: PatternLayoutScope ->
    val centerY = scope.containerSize.height / 2f
    List(18) { index ->
        val fraction = index / 17f
        val angle = fraction * PI.toFloat() * 3f
        OffsetFraction(
            x = fraction,
            y = ((centerY + sin(angle) * scope.containerSize.height * 0.18f) / scope.containerSize.height).coerceIn(0.08f, 0.92f),
        )
    }
}

@Composable
private fun rememberDemoSpritePainter(): Painter = remember {
    object : Painter() {
        override val intrinsicSize: Size = Size.Unspecified

        override fun DrawScope.onDraw() {
            val path = Path().apply {
                val outerRadius = size.minDimension / 2f
                val innerRadius = outerRadius * 0.45f
                val center = Offset(size.width / 2f, size.height / 2f)
                repeat(10) { step ->
                    val angle = (-90f + step * 36f) * (PI / 180f).toFloat()
                    val radius = if (step % 2 == 0) outerRadius else innerRadius
                    val point = Offset(
                        x = center.x + cos(angle) * radius,
                        y = center.y + sin(angle) * radius,
                    )
                    if (step == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
                close()
            }
            drawPath(path = path, color = Color.White)
        }
    }
}
