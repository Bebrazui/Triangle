package org.triangle.calculator.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Дополнительные иконки калькулятора в той же сетке и толщине штриха,
 * что и Phosphor (256x256, stroke 16).
 */
object CalcIcons {

    /** Backspace — клавиша удаления последнего символа. */
    val Backspace: ImageVector
        get() = ImageVector.Builder(
            name = "Backspace",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(136f, 64f)
                lineTo(216f, 64f)
                lineTo(216f, 192f)
                lineTo(136f, 192f)
                lineTo(40f, 128f)
                close()
                moveTo(164f, 104f); lineTo(196f, 152f)
                moveTo(196f, 104f); lineTo(164f, 152f)
            }
        }.build()

    /** Copy — копирование результата. */
    val Copy: ImageVector
        get() = ImageVector.Builder(
            name = "Copy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(96f, 96f)
                lineTo(208f, 96f)
                lineTo(208f, 208f)
                lineTo(96f, 208f)
                close()
                moveTo(64f, 168f)
                lineTo(48f, 168f)
                lineTo(48f, 48f)
                lineTo(168f, 48f)
                lineTo(168f, 64f)
            }
        }.build()

    /** Clock с обратной стрелкой — история вычислений. */
    val History: ImageVector
        get() = ImageVector.Builder(
            name = "History",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(128f, 136f); lineTo(128f, 88f)
                moveTo(167.6f, 167.6f); lineTo(128f, 128f)
                moveTo(128f, 200f)
                arcTo(72f, 72f, 0f, true, true, 186.9f, 69.1f)
                moveTo(186.9f, 69.1f); lineTo(186.9f, 24f)
                moveTo(186.9f, 69.1f); lineTo(232f, 69.1f)
            }
        }.build()
}
