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

private val DarkColorScheme = darkColorScheme(
    primary = CoralElectric,
    onPrimary = Color.White,
    primaryContainer = CoralContainerDark,
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = CyanCyber,
    onSecondary = Color(0xFF002029),
    secondaryContainer = CyanContainerDark,
    onSecondaryContainer = Color(0xFFB8F2FF),
    tertiary = AmberWarm,
    onTertiary = Color(0xFF281A00),
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = Color(0xFFFFE5A3),
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = SlateSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = SlateBorder,
    outlineVariant = Color(0xFF281F44),
    error = ErrorRedDark,
    onError = Color(0xFF2D0000)
)

private val LightColorScheme = lightColorScheme(
    primary = CoralDeep,
    onPrimary = Color.White,
    primaryContainer = CoralContainerLight,
    onPrimaryContainer = Color(0xFF3E0018),
    secondary = CyanDeep,
    onSecondary = Color.White,
    secondaryContainer = CyanContainerLight,
    onSecondaryContainer = Color(0xFF001F2A),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = Color(0xFF311900),
    background = StudioWhite,
    onBackground = TextPrimaryLight,
    surface = StudioCard,
    onSurface = TextPrimaryLight,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = StudioBorder,
    outlineVariant = Color(0xFFECE6FA),
    error = ErrorRedLight,
    onError = Color.White
)

val RadarShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun RadarViralTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = RadarShapes,
        content = content
    )
}
