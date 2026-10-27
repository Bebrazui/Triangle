package org.triangle.calculator.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.addPathNodes
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

    /** ClockCounterClockwise — точный контур Phosphor Regular. */
    val History: ImageVector
        get() = ImageVector.Builder(
            name = "History",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                stroke = null
            ) {
                addPathNodes(
                    "M136,80v43.47l36.12,21.67a8,8,0,0,1-8.24,13.72l-40-24A8,8,0,0,1,120,128V80a8,8,0,0,1,16,0Z" +
                        "M128,32A95.44,95.44,0,0,0,60.08,60.15C52.81,67.51,46.35,74.59,40,82V64a8,8,0,0,0-16,0v40a8,8,0,0,0,8,8H72a8,8,0,0,0,0-16H49c7.15-8.42,14.27-16.35,22.39-24.57a80,80,0,1,1,1.66,114.75,8,8,0,1,0-11,11.64A96,96,0,1,0,128,32Z"
                )
            }
        }.build()
}
