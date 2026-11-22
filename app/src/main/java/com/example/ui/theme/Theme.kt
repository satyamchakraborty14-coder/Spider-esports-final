package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SpiderDarkColorScheme = darkColorScheme(
    primary = SpiderRed,
    onPrimary = Color.White,
    primaryContainer = SpiderRedContainer,
    onPrimaryContainer = Color.White,
    secondary = WalletPurpleStart,
    onSecondary = Color.White,
    secondaryContainer = WalletPurpleEnd,
    onSecondaryContainer = Color.White,
    tertiary = SpiderGold,
    onTertiary = SpiderBlack,
    background = SpiderBlack,
    onBackground = TextPrimary,
    surface = SpiderSurface,
    onSurface = TextPrimary,
    surfaceVariant = SpiderSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = SpiderCardBorder,
    error = SpiderRedBright,
    onError = Color.White
)

private val SpiderShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun SpiderEsportsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SpiderDarkColorScheme,
        typography = SpiderTypography,
        shapes = SpiderShapes,
        content = content
    )
}
