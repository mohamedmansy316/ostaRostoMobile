package com.ostarosto.app.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Osta Rosto brand palette, taken from the web project's
 * resources/css/variables.css.
 */
object OstaColors {
    val Maroon = Color(0xFF7E0C3E) // --primary-color
    val MaroonDark = Color(0xFF58082A) // --primary-hover
    val MaroonTint = Color(0xFFFFF5F5) // --primary-light
    val Yellow = Color(0xFFFFE578) // --secondary-color
    val Cream = Color(0xFFFFEEDE) // --secondary-light
    val Orange = Color(0xFFF47832) // --brand-tertiary
    val Slate = Color(0xFF2D405D) // price text on the web card
    val Ink = Color(0xFF111111) // --text-primary
    val Muted = Color(0xFF64748B) // product description
    val Hairline = Color(0xFFF0F0F0) // card border
    val Success = Color(0xFF38A169)
}

private val LightColors = lightColorScheme(
    primary = OstaColors.Maroon,
    onPrimary = Color.White,
    primaryContainer = OstaColors.MaroonTint,
    onPrimaryContainer = OstaColors.MaroonDark,
    secondary = OstaColors.Yellow,
    onSecondary = OstaColors.Maroon,
    secondaryContainer = OstaColors.Yellow,
    onSecondaryContainer = OstaColors.Maroon,
    tertiary = OstaColors.Orange,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = OstaColors.Ink,
    surface = Color.White,
    onSurface = OstaColors.Ink,
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = OstaColors.Muted,
    outline = Color(0xFFCBD5E0),
    outlineVariant = OstaColors.Hairline,
    error = Color(0xFFC5341E),
)

private val DarkColors = darkColorScheme(
    primary = OstaColors.Yellow,
    onPrimary = OstaColors.Maroon,
    primaryContainer = OstaColors.MaroonDark,
    onPrimaryContainer = OstaColors.Yellow,
    secondary = OstaColors.Yellow,
    onSecondary = OstaColors.Maroon,
    tertiary = OstaColors.Orange,
    onTertiary = Color.White,
    background = Color(0xFF141013),
    onBackground = Color(0xFFF3ECEF),
    surface = Color(0xFF1E181B),
    onSurface = Color(0xFFF3ECEF),
    surfaceVariant = Color(0xFF2A2226),
    onSurfaceVariant = Color(0xFFB8ABB1),
    outline = Color(0xFF564A50),
    outlineVariant = Color(0xFF3A3035),
    error = Color(0xFFE5645A),
)

/**
 * App theme. Forces RTL for the whole tree — the app is Arabic-only — and uses
 * the bundled Cairo type scale (see [ostaTypography]).
 */
@Composable
fun OstaRostoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ostaTypography(),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            content()
        }
    }
}
