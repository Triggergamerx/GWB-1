package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val GwbLightColorScheme = lightColorScheme(
    primary = BlossomPink,
    onPrimary = Color.White,
    primaryContainer = BlossomPinkLight,
    onPrimaryContainer = BlossomPinkDark,
    secondary = MeadowMint,
    onSecondary = Color.White,
    secondaryContainer = MeadowMintLight,
    onSecondaryContainer = MeadowMintDark,
    tertiary = HoneyGold,
    onTertiary = DeepCocoaInk,
    tertiaryContainer = HoneyGoldLight,
    onTertiaryContainer = HoneyGoldDark,
    background = CreamCanvas,
    onBackground = DeepCocoaInk,
    surface = WarmSurface,
    onSurface = DeepCocoaInk,
    surfaceVariant = SoftPeachSurface,
    onSurfaceVariant = MutedCocoaInk
)

private val GwbDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF7EB6),
    onPrimary = DeepCocoaInk,
    primaryContainer = BlossomPinkDark,
    onPrimaryContainer = BlossomPinkLight,
    secondary = Color(0xFF5EEAD4),
    onSecondary = DeepCocoaInk,
    secondaryContainer = MeadowMintDark,
    onSecondaryContainer = MeadowMintLight,
    tertiary = HoneyGold,
    onTertiary = DeepCocoaInk,
    tertiaryContainer = HoneyGoldDark,
    onTertiaryContainer = HoneyGoldLight,
    background = DarkCreamCanvas,
    onBackground = Color(0xFFFFEAF2),
    surface = DarkWarmSurface,
    onSurface = Color(0xFFFFEAF2),
    surfaceVariant = DarkSoftSurface,
    onSurfaceVariant = Color(0xFFD9B8C8)
)

val GwbShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun GwbTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) GwbDarkColorScheme else GwbLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = GwbTypography,
        shapes = GwbShapes,
        content = content
    )
}
