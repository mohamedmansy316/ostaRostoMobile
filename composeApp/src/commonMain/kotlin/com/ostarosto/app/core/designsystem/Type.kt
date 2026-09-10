package com.ostarosto.app.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import ostarostomobile.composeapp.generated.resources.Res
import ostarostomobile.composeapp.generated.resources.cairo

/**
 * Cairo — a modern geometric Arabic + Latin UI typeface (SIL OFL 1.1), bundled as
 * a single variable font. Each weight is pinned through [FontVariation] so the one
 * file covers regular → bold identically on Android and iOS.
 */
@Composable
fun ostaFontFamily(): FontFamily = FontFamily(
    Font(Res.font.cairo, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(Res.font.cairo, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(Res.font.cairo, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(Res.font.cairo, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

/** The Material 3 type scale with every slot switched to [ostaFontFamily]. */
@Composable
fun ostaTypography(): Typography {
    val cairo = ostaFontFamily()
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = cairo),
        displayMedium = base.displayMedium.copy(fontFamily = cairo),
        displaySmall = base.displaySmall.copy(fontFamily = cairo),
        headlineLarge = base.headlineLarge.copy(fontFamily = cairo),
        headlineMedium = base.headlineMedium.copy(fontFamily = cairo),
        headlineSmall = base.headlineSmall.copy(fontFamily = cairo),
        titleLarge = base.titleLarge.copy(fontFamily = cairo),
        titleMedium = base.titleMedium.copy(fontFamily = cairo),
        titleSmall = base.titleSmall.copy(fontFamily = cairo),
        bodyLarge = base.bodyLarge.copy(fontFamily = cairo),
        bodyMedium = base.bodyMedium.copy(fontFamily = cairo),
        bodySmall = base.bodySmall.copy(fontFamily = cairo),
        labelLarge = base.labelLarge.copy(fontFamily = cairo),
        labelMedium = base.labelMedium.copy(fontFamily = cairo),
        labelSmall = base.labelSmall.copy(fontFamily = cairo),
    )
}
