package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DropZoneColorScheme = darkColorScheme(
    primary = FlameOrange,
    onPrimary = ObsidianBg,
    primaryContainer = FlameOrangeContainer,
    onPrimaryContainer = FlameOrangeBright,
    secondary = ElectricCyan,
    onSecondary = ObsidianBg,
    secondaryContainer = ElectricCyanContainer,
    onSecondaryContainer = ElectricCyan,
    tertiary = BooyahGold,
    onTertiary = ObsidianBg,
    tertiaryContainer = BooyahGoldContainer,
    onTertiaryContainer = BooyahGold,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHigh = ObsidianCardElevated,
    outline = OutlineSubtle,
    error = StormCrimson,
    onError = TextPrimary,
    errorContainer = StormCrimsonContainer,
    onErrorContainer = StormCrimson
)

private val DropZoneShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DropZoneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DropZoneColorScheme,
        typography = DropZoneTypography,
        shapes = DropZoneShapes,
        content = content
    )
}
