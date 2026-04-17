package com.example.alohi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════
// AloHi Typography System
// Apple SF Pro inspired — clean, readable, elegant
// Uses system default (San Francisco on most devices)
// Replace with Inter when font files are added to res/font/
// ═══════════════════════════════════════════════════════

val AloHiFontFamily = FontFamily.Default

val AloHiTypography = Typography(
    // ── Display ──
    displayLarge = TextStyle(
        fontFamily = AloHiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = 0.25.sp
    ),
    displayMedium = TextStyle(
        fontFamily = AloHiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = AloHiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),

    // ── Headlines ──
    headlineLarge = TextStyle(
        fontFamily = AloHiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AloHiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AloHiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),

    // ── Titles ──
    titleLarge = TextStyle(
        fontFamily = AloHiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,     // iOS navigation title size
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp
    ),
    titleMedium = TextStyle(
        fontFamily = AloHiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,     // Contact name in chat list
        lineHeight = 22.sp,
        letterSpacing = (-0.32).sp
    ),
    titleSmall = TextStyle(
        fontFamily = AloHiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.24).sp
    ),

    // ── Body ──
    bodyLarge = TextStyle(
        fontFamily = AloHiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,     // Main body text (iOS body)
        lineHeight = 22.sp,
        letterSpacing = (-0.41).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AloHiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,     // Secondary body / last message
        lineHeight = 20.sp,
        letterSpacing = (-0.24).sp
    ),
    bodySmall = TextStyle(
        fontFamily = AloHiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,     // Footnote
        lineHeight = 18.sp,
        letterSpacing = (-0.08).sp
    ),

    // ── Labels ──
    labelLarge = TextStyle(
        fontFamily = AloHiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,     // Button text
        lineHeight = 20.sp,
        letterSpacing = (-0.24).sp
    ),
    labelMedium = TextStyle(
        fontFamily = AloHiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,     // Tab bar labels
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AloHiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,     // Timestamp, badge count
        lineHeight = 13.sp,
        letterSpacing = 0.06.sp
    )
)