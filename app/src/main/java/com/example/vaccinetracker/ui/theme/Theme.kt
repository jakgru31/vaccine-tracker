// Theme.kt
package com.example.vaccinetracker.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

private val LightColorScheme = lightColorScheme(
    primary = LightPrimaryColor,
    onPrimary = LightOnPrimaryColor,
    primaryContainer = LightPrimaryVariantColor,
    secondary = LightSecondaryColor,
    onSecondary = LightOnSecondaryColor,
    background = LightBackgroundColor,
    onBackground = LightOnBackgroundColor,
    surface = LightSurfaceColor,
    onSurface = LightOnSurfaceColor,
    error = LightErrorColor,
    onError = LightOnErrorColor
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimaryColor,
    onPrimary = DarkOnPrimaryColor,
    primaryContainer = DarkPrimaryVariantColor,
    secondary = DarkSecondaryColor,
    onSecondary = DarkOnSecondaryColor,
    background = DarkBackgroundColor,
    onBackground = DarkOnBackgroundColor,
    surface = DarkSurfaceColor,
    onSurface = DarkOnSurfaceColor,
    error = DarkErrorColor,
    onError = DarkOnErrorColor
)

@Composable
fun VaccineTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}