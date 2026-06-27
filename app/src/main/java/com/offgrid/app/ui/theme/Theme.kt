package com.offgrid.app.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = OffGridPrimary,
    onPrimary = OffGridOnPrimary,
    primaryContainer = OffGridPrimaryContainer,
    onPrimaryContainer = OffGridOnPrimaryContainer,
    secondary = OffGridSecondary,
    onSecondary = OffGridOnSecondary,
    secondaryContainer = OffGridSecondaryContainer,
    onSecondaryContainer = OffGridOnSecondaryContainer,
    error = OffGridError,
    onError = OffGridOnError,
    errorContainer = OffGridErrorContainer,
    onErrorContainer = OffGridOnErrorContainer,
    background = OffGridBackground,
    onBackground = OffGridOnBackground,
    surface = OffGridSurface,
    onSurface = OffGridOnSurface,
    surfaceVariant = OffGridSurfaceVariant,
    onSurfaceVariant = OffGridOnSurfaceVariant,
    outline = OffGridOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = OffGridPrimaryDark,
    onPrimary = OffGridOnPrimaryDark,
    primaryContainer = OffGridPrimaryContainerDark,
    onPrimaryContainer = OffGridOnPrimaryContainerDark,
    secondary = OffGridSecondaryDark,
    onSecondary = OffGridOnSecondaryDark,
    secondaryContainer = OffGridSecondaryContainerDark,
    onSecondaryContainer = OffGridOnSecondaryContainerDark,
    error = OffGridErrorDark,
    onError = OffGridOnErrorDark,
    errorContainer = OffGridErrorContainerDark,
    onErrorContainer = OffGridOnErrorContainerDark,
    background = OffGridBackgroundDark,
    onBackground = OffGridOnBackgroundDark,
    surface = OffGridSurfaceDark,
    onSurface = OffGridOnSurfaceDark,
    surfaceVariant = OffGridSurfaceVariantDark,
    onSurfaceVariant = OffGridOnSurfaceVariantDark,
    outline = OffGridOutlineDark
)

@Composable
fun OffGridTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
