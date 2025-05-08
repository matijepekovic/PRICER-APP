package com.example.pricer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Defines the light color scheme using the colors from Color.kt
private val LightColors = lightColorScheme(
    primary = Purple40,
    onPrimary = OnPrimaryLight,
    secondary = PurpleGrey40,
    onSecondary = OnSecondaryLight,
    tertiary = Pink40,
    onTertiary = OnTertiaryLight,
    error = Error40,
    onError = OnErrorLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceLight, // Often same as surface or slightly different tint
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    // inverseOnSurface = Color.Black, // For specific accessibility needs if required
    // inverseSurface = Color.White,
    // inversePrimary = Color(0xFF...),
    // surfaceTint = Purple40 // Color overlay on surface when elevated
)

// Defines the dark color scheme using the colors from Color.kt
private val DarkColors = darkColorScheme(
    primary = Purple80,
    onPrimary = OnPrimaryDark,
    secondary = PurpleGrey80,
    onSecondary = OnSecondaryDark,
    tertiary = Pink80,
    onTertiary = OnTertiaryDark,
    error = Error80,
    onError = OnErrorDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceDark, // Often same as surface or slightly different tint
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    // inverseOnSurface = Color.White,
    // inverseSurface = Color.Black,
    // inversePrimary = Color(0xFF...),
    // surfaceTint = Purple80
)

/**
 * The main theme composable for the Pricer application.
 * Applies the appropriate color scheme (light/dark, dynamic), typography, and sets up
 * system bar coloring.
 *
 * @param darkTheme Whether the theme should be forced into dark mode. Defaults to system setting.
 * @param dynamicColor Whether to use Material You dynamic colors on Android 12+. Defaults to true.
 * @param content The composable content to which the theme will be applied.
 */
@Composable
fun PricerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Set to false to disable Material You
    content: @Composable () -> Unit
) {
    // Determine the base color scheme
    val baseColorScheme = when {
        // Use dynamic colors if available and enabled
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Fallback to predefined dark/light schemes
        darkTheme -> DarkColors
        else -> LightColors
    }

    // Get the current view and window for system bar customization
    val view = LocalView.current
    if (!view.isInEditMode) {
        // Perform side effect to change system bar colors to match the theme
        SideEffect {
            val window = (view.context as Activity).window // Get window from Activity context

            // Set status bar color
            window.statusBarColor = baseColorScheme.primary.toArgb() // Example: Use primary color

            // Set status bar icon/text appearance (light or dark icons)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme

            // Optionally set navigation bar color and appearance as well
            // window.navigationBarColor = baseColorScheme.surface.toArgb() // Example: Use surface
            // WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    // Apply the determined color scheme and typography to the MaterialTheme
    MaterialTheme(
        colorScheme = baseColorScheme,
        typography = AppTypography, // Use the typography defined in Type.kt
        // You can also override shapes here if needed: shapes = AppShapes,
        content = content // Render the actual UI content within the theme
    )
}