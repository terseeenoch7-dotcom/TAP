package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TapColorScheme = darkColorScheme(
    primary = TapGoldPrimary,
    onPrimary = TapOnGold,
    primaryContainer = TapGoldContainer,
    onPrimaryContainer = TapGoldLight,
    secondary = TapOrangeAccent,
    onSecondary = TapBlack,
    secondaryContainer = TapSurfaceVariant,
    onSecondaryContainer = TapTextPrimary,
    tertiary = TapGreenSuccess,
    onTertiary = TapBlack,
    background = TapBlack,
    onBackground = TapTextPrimary,
    surface = TapDarkCharcoal,
    onSurface = TapTextPrimary,
    surfaceVariant = TapSurfaceVariant,
    onSurfaceVariant = TapTextSecondary,
    outline = TapOutline,
    error = TapRedAlert,
    onError = TapBlack
)

@Composable
fun TAPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Online-first fintech tap games require an intentional, consistent dark luxury theme
    MaterialTheme(
        colorScheme = TapColorScheme,
        typography = Typography,
        content = content
    )
}
