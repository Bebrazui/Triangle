package org.triangle.settings.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Neo-Editorial Светлая палитра (Editorial Brutalism)
val LightPaperBackground = Color(0xFFF7F5EE)  // Плотный теплый холст
val LightSurfaceCard = Color(0xFFFFFFFF)      // Чистый белый для карточек
val LightBorderColor = Color(0xFFDED8CB)      // Четкий контур разделителей
val LightBorderBold = Color(0xFF191816)       // Глубокий типографский черный
val LightTextPrimary = Color(0xFF191816)      // 100% глубокий черный
val LightTextSecondary = Color(0xFF4A4642)    // Заметно затемненный графит
val LightTextTertiary = Color(0xFF706B65)     // Четкий второстепенный

// Neo-Editorial Темная палитра (Editorial Dark / Quiet Luxury)
val DarkPaperBackground = Color(0xFF0F0E0D)   // Глубокий матовый темный холст
val DarkSurfaceCard = Color(0xFF1B1A18)       // Премиальная контрастная поверхность карточек
val DarkBorderColor = Color(0xFF2C2A26)       // Тонкий благородный контур
val DarkBorderBold = Color(0xFFF7F5EE)        // Контрастный светлый контур
val DarkTextPrimary = Color(0xFFF5F3EC)       // Мягкий матовый белый текст
val DarkTextSecondary = Color(0xFFB5AFA6)     // Теплый серебристо-серый
val DarkTextTertiary = Color(0xFF7A756D)      // Приглушенный графит

val AccentTerracotta = Color(0xFFBD4A26)
val SoftOchre = Color(0xFFC26E00)

data class TriangleColors(
    val paperBackground: Color,
    val surfaceCard: Color,
    val borderColor: Color,
    val borderBold: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accentTerracotta: Color = AccentTerracotta,
    val softOchre: Color = SoftOchre
)

val LightTriangleColors = TriangleColors(
    paperBackground = LightPaperBackground,
    surfaceCard = LightSurfaceCard,
    borderColor = LightBorderColor,
    borderBold = LightBorderBold,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary
)

val DarkTriangleColors = TriangleColors(
    paperBackground = DarkPaperBackground,
    surfaceCard = DarkSurfaceCard,
    borderColor = DarkBorderColor,
    borderBold = DarkBorderBold,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textTertiary = DarkTextTertiary
)

val LocalTriangleColors = compositionLocalOf { LightTriangleColors }

// Динамические геттеры для прямого использования в Composable
val PaperBackground: Color
    @Composable get() = LocalTriangleColors.current.paperBackground

val SurfaceCard: Color
    @Composable get() = LocalTriangleColors.current.surfaceCard

val BorderColor: Color
    @Composable get() = LocalTriangleColors.current.borderColor

val BorderBold: Color
    @Composable get() = LocalTriangleColors.current.borderBold

val TextPrimary: Color
    @Composable get() = LocalTriangleColors.current.textPrimary

val TextSecondary: Color
    @Composable get() = LocalTriangleColors.current.textSecondary

val TextTertiary: Color
    @Composable get() = LocalTriangleColors.current.textTertiary

val AccentColor: Color
    @Composable get() = LocalTriangleColors.current.accentTerracotta
