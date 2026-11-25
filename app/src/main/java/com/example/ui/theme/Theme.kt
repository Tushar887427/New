package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val WorkedLightColors = lightColorScheme(
    primary = Brand600,
    onPrimary = Color.White,
    primaryContainer = Brand50,
    onPrimaryContainer = Brand700,
    secondary = Accent500,
    onSecondary = Color.White,
    secondaryContainer = TintPeachLight,
    onSecondaryContainer = Accent600,
    tertiary = Brand500,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Slate900,
    surface = LightSurface,
    onSurface = Slate900,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Slate600,
    outline = LightBorder,
    outlineVariant = Slate200,
    error = Rose600,
    onError = Color.White
)

private val WorkedDarkColors = darkColorScheme(
    primary = BrandDarkAccent,
    onPrimary = Color(0xFF042230),
    primaryContainer = TintSkyDark,
    onPrimaryContainer = Color(0xFF7DD3FC),
    secondary = Accent500,
    onSecondary = Color.White,
    secondaryContainer = TintPeachDark,
    onSecondaryContainer = Color(0xFFFDBA74),
    tertiary = Brand500,
    onTertiary = Color.White,
    background = AmoledBackground,
    onBackground = Color.White,
    surface = AmoledSurface,
    onSurface = Color.White,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = AmoledBorder,
    outlineVariant = Color(0x1EFFFFFF),
    error = Color(0xFFFDA4AF),
    onError = Color(0xFF4C0519)
)

val WorkedShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun WorkedTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) WorkedDarkColors else WorkedLightColors,
        typography = WorkedTypography,
        shapes = WorkedShapes,
        content = content
    )
}
