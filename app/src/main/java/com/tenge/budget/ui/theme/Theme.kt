package com.tenge.budget.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Brand600,
    onPrimary = CardSurface,
    primaryContainer = Brand50,
    onPrimaryContainer = Brand800,
    secondary = Danger,
    onSecondary = CardSurface,
    secondaryContainer = DangerContainerLight,
    onSecondaryContainer = Color(0xFF7A2E1F),
    tertiary = Accent,
    onTertiary = CardSurface,
    tertiaryContainer = AccentContainerLight,
    onTertiaryContainer = Color(0xFF6B4A12),
    background = Paper,
    onBackground = Ink,
    surface = CardSurface,
    onSurface = Ink,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = InkSecondary,
    outline = OutlineLight,
    outlineVariant = Color(0xFFEDEBE3),
    error = ErrorRed,
    onError = CardSurface,
    errorContainer = DangerContainerLight,
    onErrorContainer = Color(0xFF5C2415),
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    inversePrimary = Brand300
)

private val DarkColors = darkColorScheme(
    primary = Brand300,
    onPrimary = Color(0xFF07231C),
    primaryContainer = Brand800,
    onPrimaryContainer = Brand50,
    secondary = DangerDark,
    onSecondary = Color(0xFF4A1A10),
    secondaryContainer = DangerContainerDark,
    onSecondaryContainer = Color(0xFFFFDDD3),
    tertiary = Color(0xFFFFC961),
    onTertiary = Color(0xFF4E3B12),
    tertiaryContainer = AccentContainerDark,
    onTertiaryContainer = Color(0xFFFFEBC2),
    background = InkDarkBg,
    onBackground = ParchmentText,
    surface = CardSurfaceDark,
    onSurface = ParchmentText,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = ParchmentTextSecondary,
    outline = OutlineDark,
    outlineVariant = Color(0xFF29302F),
    error = ErrorRedDark,
    onError = Color(0xFF4A1A10),
    errorContainer = Color(0xFF5C2A1F),
    onErrorContainer = Color(0xFFFFDDD3),
    inverseSurface = ParchmentText,
    inverseOnSurface = InkDarkBg,
    inversePrimary = Brand600
)

/**
 * App theme. Dynamic (Material You) color is off by default — the app shares
 * a deliberate brand identity with the web client (warm paper canvas, near-
 * black ink text, one confident green) that a wallpaper-derived palette
 * would flatten. Callers can still opt in with [dynamicColor] if they want
 * to experiment.
 */
@Composable
fun TengeBudgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                androidx.compose.material3.dynamicDarkColorScheme(context)
            } else {
                androidx.compose.material3.dynamicLightColorScheme(context)
            }
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TengeTypography,
        shapes = TengeShapes,
        content = content
    )
}
