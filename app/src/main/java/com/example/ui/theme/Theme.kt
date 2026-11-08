package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TacticalColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = TacticalSurfaceVariant,
    onPrimaryContainer = NeonCyan,
    secondary = AmberGold,
    onSecondary = Color.Black,
    secondaryContainer = TacticalSurfaceVariant,
    onSecondaryContainer = AmberGold,
    tertiary = CrimsonRed,
    onTertiary = Color.White,
    background = TacticalDark,
    onBackground = TextWhite,
    surface = TacticalSurface,
    onSurface = TextWhite,
    surfaceVariant = TacticalSurfaceVariant,
    onSurfaceVariant = TextMuted,
    outline = TacticalBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = TacticalColorScheme,
        typography = Typography,
        content = content
    )
}
