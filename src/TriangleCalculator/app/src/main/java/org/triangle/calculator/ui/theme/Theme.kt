package org.triangle.calculator.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Тема калькулятора повторяет TriangleSettings:
 * системная тёмная тема + та же настройка Monet из общих prefs.
 */
@Composable
fun TriangleCalculatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dynamicColor = remember(context) { isDynamicColorEnabled(context) }

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

/** Читает тот же флаг Monet, что и TriangleSettings. */
private fun isDynamicColorEnabled(context: Context): Boolean {
    return try {
        context.getSharedPreferences("triangle_settings_prefs", Context.MODE_PRIVATE)
            .getBoolean("enable_monet_theme", true)
    } catch (e: Exception) {
        true
    }
}
