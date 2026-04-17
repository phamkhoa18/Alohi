package com.example.alohi.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ═══════════════════════════════════════════════════════
// AloHi Extended Color Tokens (beyond M3)
// ═══════════════════════════════════════════════════════
data class AloHiColorScheme(
    val bubbleSender: Color,
    val bubbleReceiver: Color,
    val bubbleTextSender: Color,
    val bubbleTextReceiver: Color,
    val divider: Color,
    val border: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val online: Color,
    val accent: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
)

val LocalAloHiColors = staticCompositionLocalOf {
    AloHiColorScheme(
        bubbleSender = LightBubbleSender,
        bubbleReceiver = LightBubbleReceiver,
        bubbleTextSender = LightBubbleTextSender,
        bubbleTextReceiver = LightBubbleTextReceiver,
        divider = LightDivider,
        border = LightBorder,
        textSecondary = LightTextSecondary,
        textTertiary = LightTextTertiary,
        online = AloHiGreen,
        accent = AloHiIndigo,
        gradientStart = AloHiBlue,
        gradientEnd = AloHiCyan,
    )
}

// ── M3 Light Color Scheme ──
private val LightColorScheme = lightColorScheme(
    primary = AloHiBlue,
    onPrimary = Color.White,
    primaryContainer = AloHiBlueLight,
    onPrimaryContainer = AloHiBlueDark,
    secondary = AloHiCyan,
    onSecondary = Color.White,
    secondaryContainer = AloHiCyanLight,
    onSecondaryContainer = AloHiBlueDark,
    tertiary = AloHiIndigo,
    onTertiary = Color.White,
    tertiaryContainer = AloHiIndigoLight,
    onTertiaryContainer = Color.White,
    error = AloHiRed,
    onError = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerHighest = LightSurfaceContainer,
    outline = LightBorder,
    outlineVariant = LightDivider,
)

// ── M3 Dark Color Scheme ──
private val DarkColorScheme = darkColorScheme(
    primary = AloHiBlue,
    onPrimary = Color.White,
    primaryContainer = AloHiBlueDark,
    onPrimaryContainer = AloHiBlueLight,
    secondary = AloHiCyan,
    onSecondary = Color.White,
    secondaryContainer = AloHiBlueDark,
    onSecondaryContainer = AloHiCyanLight,
    tertiary = AloHiIndigo,
    onTertiary = Color.White,
    tertiaryContainer = AloHiIndigoLight,
    onTertiaryContainer = Color.White,
    error = AloHiRedLight,
    onError = Color.White,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainerHighest = DarkSurfaceContainer,
    outline = DarkBorder,
    outlineVariant = DarkDivider,
)

// ── Extended color sets ──
private val LightExtendedColors = AloHiColorScheme(
    bubbleSender = LightBubbleSender,
    bubbleReceiver = LightBubbleReceiver,
    bubbleTextSender = LightBubbleTextSender,
    bubbleTextReceiver = LightBubbleTextReceiver,
    divider = LightDivider,
    border = LightBorder,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary,
    online = AloHiGreen,
    accent = AloHiIndigo,
    gradientStart = AloHiBlue,
    gradientEnd = AloHiCyan,
)

private val DarkExtendedColors = AloHiColorScheme(
    bubbleSender = DarkBubbleSender,
    bubbleReceiver = DarkBubbleReceiver,
    bubbleTextSender = DarkBubbleTextSender,
    bubbleTextReceiver = DarkBubbleTextReceiver,
    divider = DarkDivider,
    border = DarkBorder,
    textSecondary = DarkTextSecondary,
    textTertiary = DarkTextTertiary,
    online = AloHiGreenLight,
    accent = AloHiIndigoLight,
    gradientStart = AloHiBlue,
    gradientEnd = AloHiCyan,
)

@Composable
fun AloHiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled — we use our own brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    // Status bar & navigation bar styling
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalAloHiColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AloHiTypography,
            shapes = AloHiShapes,
            content = content
        )
    }
}

// ── Convenience accessor ──
object AloHiTheme {
    val extendedColors: AloHiColorScheme
        @Composable
        get() = LocalAloHiColors.current
}