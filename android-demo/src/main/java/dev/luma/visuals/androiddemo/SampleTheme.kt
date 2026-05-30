package dev.luma.visuals.androiddemo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun SampleTheme(content: @Composable () -> Unit) {
    val darkScheme = darkColorScheme(
        primary = Color(0xFF8AA8FF),
        secondary = Color(0xFF72E6C8),
        tertiary = Color(0xFFFF9AC2),
        background = Color(0xFF050816),
        surface = Color(0xFF0C1226),
        surfaceVariant = Color(0xFF111B32),
    )
    val lightScheme = lightColorScheme(
        primary = Color(0xFF3556D4),
        secondary = Color(0xFF007B62),
        tertiary = Color(0xFFC13B70),
    )
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme,
        content = content,
    )
}
