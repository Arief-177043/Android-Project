package com.growwx.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Brand Colors ─────────────────────────────────────────────────────────────

object GrowwXColor {
    val Green = Color(0xFF00C896)
    val GreenDark = Color(0xFF00A87A)
    val GreenLight = Color(0xFFE6FAF5)
    val GreenContainer = Color(0xFF0A2A22)

    val Red = Color(0xFFFF4B6E)
    val RedLight = Color(0xFFFFF0F3)
    val RedContainer = Color(0xFF2A0F18)

    val Blue = Color(0xFF3B82F6)
    val BlueLight = Color(0xFFEFF6FF)

    val Amber = Color(0xFFF59E0B)
    val AmberLight = Color(0xFFFFFBEB)

    val Purple = Color(0xFF8B5CF6)
    val PurpleLight = Color(0xFFF5F3FF)

    // Light theme
    val Background = Color(0xFFF8FAFA)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF4F7F8)
    val Border = Color(0xFFE8EEF0)
    val TextPrimary = Color(0xFF0A1628)
    val TextSecondary = Color(0xFF4A5568)
    val TextMuted = Color(0xFF8A97A8)

    // Dark theme
    val DarkBackground = Color(0xFF0A0F1A)
    val DarkSurface = Color(0xFF111827)
    val DarkSurfaceVariant = Color(0xFF1A2235)
    val DarkBorder = Color(0xFF1E2D42)
    val DarkTextPrimary = Color(0xFFF0F6FF)
    val DarkTextSecondary = Color(0xFF8BAEC8)
    val DarkTextMuted = Color(0xFF4A6070)
}

// ─── Light Color Scheme ───────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = GrowwXColor.Green,
    onPrimary = Color.White,
    primaryContainer = GrowwXColor.GreenLight,
    onPrimaryContainer = GrowwXColor.GreenDark,
    secondary = GrowwXColor.Blue,
    onSecondary = Color.White,
    error = GrowwXColor.Red,
    onError = Color.White,
    errorContainer = GrowwXColor.RedLight,
    background = GrowwXColor.Background,
    onBackground = GrowwXColor.TextPrimary,
    surface = GrowwXColor.Surface,
    onSurface = GrowwXColor.TextPrimary,
    surfaceVariant = GrowwXColor.SurfaceVariant,
    onSurfaceVariant = GrowwXColor.TextSecondary,
    outline = GrowwXColor.Border,
)

// ─── Dark Color Scheme ────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary = GrowwXColor.Green,
    onPrimary = Color.Black,
    primaryContainer = GrowwXColor.GreenContainer,
    onPrimaryContainer = GrowwXColor.Green,
    secondary = GrowwXColor.Blue,
    onSecondary = Color.Black,
    error = GrowwXColor.Red,
    onError = Color.Black,
    errorContainer = GrowwXColor.RedContainer,
    background = GrowwXColor.DarkBackground,
    onBackground = GrowwXColor.DarkTextPrimary,
    surface = GrowwXColor.DarkSurface,
    onSurface = GrowwXColor.DarkTextPrimary,
    surfaceVariant = GrowwXColor.DarkSurfaceVariant,
    onSurfaceVariant = GrowwXColor.DarkTextSecondary,
    outline = GrowwXColor.DarkBorder,
)

// ─── Typography (using system default; swap for custom font via res/font/) ────

val GrowwXTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 11.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.5.sp),
)

// ─── Extended Colors (accessible via LocalGrowwXColors) ──────────────────────

data class GrowwXExtendedColors(
    val green: Color,
    val greenLight: Color,
    val red: Color,
    val redLight: Color,
    val textMuted: Color,
    val border: Color,
    val inputBg: Color,
)

val LocalGrowwXColors = staticCompositionLocalOf {
    GrowwXExtendedColors(
        green = GrowwXColor.Green,
        greenLight = GrowwXColor.GreenLight,
        red = GrowwXColor.Red,
        redLight = GrowwXColor.RedLight,
        textMuted = GrowwXColor.TextMuted,
        border = GrowwXColor.Border,
        inputBg = GrowwXColor.SurfaceVariant,
    )
}

// ─── Theme Composable ─────────────────────────────────────────────────────────

@Composable
fun GrowwXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) {
        GrowwXExtendedColors(
            green = GrowwXColor.Green,
            greenLight = GrowwXColor.GreenContainer,
            red = GrowwXColor.Red,
            redLight = GrowwXColor.RedContainer,
            textMuted = GrowwXColor.DarkTextMuted,
            border = GrowwXColor.DarkBorder,
            inputBg = GrowwXColor.DarkSurfaceVariant,
        )
    } else {
        GrowwXExtendedColors(
            green = GrowwXColor.Green,
            greenLight = GrowwXColor.GreenLight,
            red = GrowwXColor.Red,
            redLight = GrowwXColor.RedLight,
            textMuted = GrowwXColor.TextMuted,
            border = GrowwXColor.Border,
            inputBg = GrowwXColor.SurfaceVariant,
        )
    }

    CompositionLocalProvider(LocalGrowwXColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GrowwXTypography,
            content = content
        )
    }
}

// Convenience accessor
val MaterialTheme.extendedColors: GrowwXExtendedColors
    @Composable get() = LocalGrowwXColors.current
