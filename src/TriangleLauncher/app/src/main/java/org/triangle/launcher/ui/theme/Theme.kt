package org.triangle.launcher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

@Composable
fun TriangleLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colors = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dynScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        if (darkTheme) {
            DarkTriangleColors.copy(
                paperBackground = dynScheme.background,
                surfaceCard = dynScheme.surface,
                textPrimary = dynScheme.onSurface,
                textSecondary = dynScheme.onSurfaceVariant,
                accentTerracotta = dynScheme.primary,
                softOchre = dynScheme.secondary
            )
        } else {
            LightTriangleColors.copy(
                paperBackground = dynScheme.background,
                surfaceCard = dynScheme.surface,
                textPrimary = dynScheme.onSurface,
                textSecondary = dynScheme.onSurfaceVariant,
                accentTerracotta = dynScheme.primary,
                softOchre = dynScheme.secondary
            )
        }
    } else {
        if (darkTheme) DarkTriangleColors else LightTriangleColors
    }

    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) {
            darkColorScheme(
                background = colors.paperBackground,
                surface = colors.surfaceCard,
                onBackground = colors.textPrimary,
                onSurface = colors.textPrimary,
                primary = colors.accentTerracotta,
                onPrimary = colors.paperBackground
            )
        } else {
            lightColorScheme(
                background = colors.paperBackground,
                surface = colors.surfaceCard,
                onBackground = colors.textPrimary,
                onSurface = colors.textPrimary,
                primary = colors.borderBold,
                onPrimary = colors.surfaceCard
            )
        }
    }

    CompositionLocalProvider(LocalTriangleColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = EditorialTypography,
            content = content
        )
    }
}
