package com.tattoo.studio.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.tattoo.studio.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val HeadlineFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Bodoni Moda"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Bodoni Moda"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Bodoni Moda"), fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = GoogleFont("Bodoni Moda"), fontProvider = provider, weight = FontWeight.Black)
)

val BodyFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Hanken Grotesk"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Hanken Grotesk"), fontProvider = provider, weight = FontWeight.Medium)
)

val LabelFontFamily = FontFamily(
    Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = provider, weight = FontWeight.Medium)
)

val AppTypography = Typography(
    // mapped from headline-xl
    displayLarge = TextStyle(
        fontFamily = HeadlineFontFamily,
        fontWeight = FontWeight.Black, // 900
        fontSize = 48.sp,
        lineHeight = 52.8.sp, // 48 * 1.1
        letterSpacing = (-0.02).sp
    ),
    // mapped from headline-lg
    headlineLarge = TextStyle(
        fontFamily = HeadlineFontFamily,
        fontWeight = FontWeight.Bold, // 700
        fontSize = 32.sp,
        lineHeight = 38.4.sp // 32 * 1.2
    ),
    // mapped from headline-md
    headlineMedium = TextStyle(
        fontFamily = HeadlineFontFamily,
        fontWeight = FontWeight.SemiBold, // 600
        fontSize = 24.sp,
        lineHeight = 31.2.sp // 24 * 1.3
    ),
    // mapped from body-lg
    bodyLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal, // 400
        fontSize = 18.sp,
        lineHeight = 28.8.sp // 18 * 1.6
    ),
    // mapped from body-md
    bodyMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal, // 400
        fontSize = 16.sp,
        lineHeight = 24.sp // 16 * 1.5
    ),
    // mapped from label-sm
    labelSmall = TextStyle(
        fontFamily = LabelFontFamily,
        fontWeight = FontWeight.Medium, // 500
        fontSize = 12.sp,
        lineHeight = 12.sp // 12 * 1.0
    )
)
