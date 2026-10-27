package org.triangle.calculator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Тема калькулятора использует только статичную палитру Triangle OS.
 * Monet сюда намеренно не подключён: настройка Monet в TriangleSettings
 * хранится в приватных prefs другого приложения и не является системной,
 * поэтому калькулятор не может (и не должен) её читать.
 */
@Composable
fun TriangleCalculatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkTriangleColors else LightTriangleColors

    val colorScheme = if (darkTheme) {
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

    CompositionLocalProvider(LocalTriangleColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = EditorialTypography,
            content = content
        )
    }
}
