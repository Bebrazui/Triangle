package org.triangle.calculator.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Аутентичные векторные иконки Phosphor Icons (Regular 256x256 viewbox)
 * https://phosphoricons.com
 */
object Phosphor {
    // ArrowLeft
    val ArrowLeft: ImageVector
        get() = ImageVector.Builder(
            name = "ArrowLeft",
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
                moveTo(216f, 128f); lineTo(40f, 128f)
                moveTo(112f, 56f); lineTo(40f, 128f); lineTo(112f, 200f)
            }
        }.build()

    // X (Close / Clear)
    val X: ImageVector
        get() = ImageVector.Builder(
            name = "X",
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
                moveTo(200f, 56f); lineTo(56f, 200f)
                moveTo(200f, 200f); lineTo(56f, 56f)
            }
        }.build()

    // MagnifyingGlass
    val MagnifyingGlass: ImageVector
        get() = ImageVector.Builder(
            name = "MagnifyingGlass",
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
                moveTo(112f, 40f); arcTo(72f, 72f, 0f, true, true, 40f, 112f); arcTo(72f, 72f, 0f, false, true, 112f, 40f); close()
                moveTo(162.9f, 162.9f); lineTo(216f, 216f)
            }
        }.build()

    // UserCircle (Account / Profile)
    val UserCircle: ImageVector
        get() = ImageVector.Builder(
            name = "UserCircle",
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
                moveTo(128f, 24f); arcTo(104f, 104f, 0f, true, true, 24f, 128f); arcTo(104f, 104f, 0f, false, true, 128f, 24f); close()
                moveTo(128f, 80f); arcTo(32f, 32f, 0f, true, true, 96f, 112f); arcTo(32f, 32f, 0f, false, true, 128f, 80f); close()
                moveTo(61.8f, 196.4f); curveTo(74f, 172.8f, 98.8f, 160f, 128f, 160f); curveTo(157.2f, 160f, 182f, 172.8f, 194.2f, 196.4f)
            }
        }.build()

    // Wifi
    val Wifi: ImageVector
        get() = ImageVector.Builder(
            name = "Wifi",
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
                moveTo(24f, 96f); curveTo(82f, 38f, 174f, 38f, 232f, 96f)
                moveTo(56f, 128f); curveTo(96f, 88f, 160f, 88f, 200f, 128f)
                moveTo(88f, 160f); curveTo(110f, 138f, 146f, 138f, 168f, 160f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(128f, 204f); curveTo(121.37f, 204f, 116f, 209.37f, 116f, 216f); curveTo(116f, 222.63f, 121.37f, 228f, 128f, 228f); curveTo(134.63f, 228f, 140f, 222.63f, 140f, 216f); curveTo(140f, 209.37f, 134.63f, 204f, 128f, 204f); close()
            }
        }.build()

    // Bluetooth
    val Bluetooth: ImageVector
        get() = ImageVector.Builder(
            name = "Bluetooth",
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
                moveTo(120f, 32f); lineTo(184f, 80f); lineTo(120f, 128f); lineTo(120f, 32f); close()
                moveTo(120f, 128f); lineTo(184f, 176f); lineTo(120f, 224f); lineTo(120f, 128f); close()
                moveTo(64f, 88f); lineTo(184f, 176f); moveTo(64f, 168f); lineTo(184f, 80f)
            }
        }.build()

    // Sun (Display)
    val Sun: ImageVector
        get() = ImageVector.Builder(
            name = "Sun",
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
                moveTo(128f, 80f); arcTo(48f, 48f, 0f, true, true, 80f, 128f); arcTo(48f, 48f, 0f, false, true, 128f, 80f); close()
                moveTo(128f, 24f); lineTo(128f, 48f); moveTo(128f, 208f); lineTo(128f, 232f)
                moveTo(24f, 128f); lineTo(48f, 128f); moveTo(208f, 128f); lineTo(232f, 128f)
                moveTo(54.5f, 54.5f); lineTo(71.5f, 71.5f); moveTo(184.5f, 184.5f); lineTo(201.5f, 201.5f)
                moveTo(54.5f, 201.5f); lineTo(71.5f, 184.5f); moveTo(184.5f, 71.5f); lineTo(201.5f, 54.5f)
            }
        }.build()

    // MapPin (Location)
    val MapPin: ImageVector
        get() = ImageVector.Builder(
            name = "MapPin",
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
                moveTo(128f, 224f); curveTo(128f, 224f, 208f, 144f, 208f, 104f); arcTo(80f, 80f, 0f, true, false, 48f, 104f); curveTo(48f, 144f, 128f, 224f, 128f, 224f); close()
                moveTo(128f, 72f); arcTo(32f, 32f, 0f, true, true, 96f, 104f); arcTo(32f, 32f, 0f, false, true, 128f, 72f); close()
            }
        }.build()

    // Accessibility (PersonArmsSpread in circle - authentic Phosphor)
    val Accessibility: ImageVector
        get() = ImageVector.Builder(
            name = "Accessibility",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).addPath(
            pathData = addPathNodes("M128,24A104,104,0,1,0,232,128,104.11,104.11,0,0,0,128,24Zm0,192a88,88,0,1,1,88-88A88.1,88.1,0,0,1,128,216ZM112,80a16,16,0,1,1,16,16A16,16,0,0,1,112,80Zm72,32a8,8,0,0,1-8,8H136v13.58l30.66,46a8,8,0,0,1-13.32,8.88l-25.34-38-25.34,38a8,8,0,1,1-13.32-8.88l30.66-46V120H80a8,8,0,0,1,0-16h96A8,8,0,0,1,184,112Z"),
            fill = SolidColor(Color.Black)
        ).build()

    // Palette (Wallpaper & Style - authentic Phosphor)
    val Palette: ImageVector
        get() = ImageVector.Builder(
            name = "Palette",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).addPath(
            pathData = addPathNodes("M200.77,53.89A103.27,103.27,0,0,0,128,24h-1.07A104,104,0,0,0,24,128c0,43,26.58,79.06,69.36,94.17A32,32,0,0,0,136,192a16,16,0,0,1,16-16h46.21a31.81,31.81,0,0,0,31.2-24.88,104.43,104.43,0,0,0,2.59-24A103.28,103.28,0,0,0,200.77,53.89Zm13,93.71A15.89,15.89,0,0,1,198.21,160H152a32,32,0,0,0-32,32,16,16,0,0,1-21.31,15.07C62.49,194.3,40,164,40,128a88,88,0,0,1,87.09-88h.9a88.35,88.35,0,0,1,88,87.25A88.86,88.86,0,0,1,213.81,147.6ZM140,76a12,12,0,1,1-12-12A12,12,0,0,1,140,76ZM96,100A12,12,0,1,1,84,88,12,12,0,0,1,96,100Zm0,56a12,12,0,1,1-12-12A12,12,0,0,1,96,156Zm88-56a12,12,0,1,1-12-12A12,12,0,0,1,184,100Z"),
            fill = SolidColor(Color.Black)
        ).build()

    // SpeakerHigh (Sound)
    val SpeakerHigh: ImageVector
        get() = ImageVector.Builder(
            name = "SpeakerHigh",
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
                moveTo(80f, 88f); lineTo(32f, 88f); arcTo(8f, 8f, 0f, false, false, 24f, 96f); lineTo(24f, 160f); arcTo(8f, 8f, 0f, false, false, 32f, 168f); lineTo(80f, 168f); lineTo(144f, 216f); arcTo(8f, 8f, 0f, false, false, 156f, 210f); lineTo(156f, 46f); arcTo(8f, 8f, 0f, false, false, 144f, 40f); close()
                moveTo(192f, 104f); curveTo(200f, 112f, 204f, 120f, 204f, 128f); curveTo(204f, 136f, 200f, 144f, 192f, 152f)
                moveTo(216f, 80f); curveTo(232f, 96f, 240f, 112f, 240f, 128f); curveTo(240f, 144f, 232f, 160f, 216f, 176f)
            }
        }.build()

    // Bell (Notifications)
    val Bell: ImageVector
        get() = ImageVector.Builder(
            name = "Bell",
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
                moveTo(48f, 184f); lineTo(208f, 184f)
                moveTo(96f, 216f); curveTo(96f, 224f, 110f, 232f, 128f, 232f); curveTo(146f, 232f, 160f, 224f, 160f, 216f)
                moveTo(56f, 184f); lineTo(56f, 104f); curveTo(56f, 64f, 88f, 32f, 128f, 32f); curveTo(168f, 32f, 200f, 64f, 200f, 104f); lineTo(200f, 184f)
            }
        }.build()

    // SquaresFour (Apps)
    val SquaresFour: ImageVector
        get() = ImageVector.Builder(
            name = "SquaresFour",
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
                moveTo(104f, 48f); lineTo(56f, 48f); arcTo(8f, 8f, 0f, false, false, 48f, 56f); lineTo(48f, 104f); arcTo(8f, 8f, 0f, false, false, 56f, 112f); lineTo(104f, 112f); arcTo(8f, 8f, 0f, false, false, 112f, 104f); lineTo(112f, 56f); arcTo(8f, 8f, 0f, false, false, 104f, 48f); close()
                moveTo(200f, 48f); lineTo(152f, 48f); arcTo(8f, 8f, 0f, false, false, 144f, 56f); lineTo(144f, 104f); arcTo(8f, 8f, 0f, false, false, 152f, 112f); lineTo(200f, 112f); arcTo(8f, 8f, 0f, false, false, 208f, 104f); lineTo(208f, 56f); arcTo(8f, 8f, 0f, false, false, 200f, 48f); close()
                moveTo(104f, 144f); lineTo(56f, 144f); arcTo(8f, 8f, 0f, false, false, 48f, 152f); lineTo(48f, 200f); arcTo(8f, 8f, 0f, false, false, 56f, 208f); lineTo(104f, 208f); arcTo(8f, 8f, 0f, false, false, 112f, 200f); lineTo(112f, 152f); arcTo(8f, 8f, 0f, false, false, 104f, 144f); close()
                moveTo(200f, 144f); lineTo(152f, 144f); arcTo(8f, 8f, 0f, false, false, 144f, 152f); lineTo(144f, 200f); arcTo(8f, 8f, 0f, false, false, 152f, 208f); lineTo(200f, 208f); arcTo(8f, 8f, 0f, false, false, 208f, 200f); lineTo(208f, 152f); arcTo(8f, 8f, 0f, false, false, 200f, 144f); close()
            }
        }.build()

    // BatteryCharging
    val BatteryCharging: ImageVector
        get() = ImageVector.Builder(
            name = "BatteryCharging",
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
                moveTo(248f, 112f); lineTo(248f, 144f)
                moveTo(200f, 64f); lineTo(32f, 64f); arcTo(16f, 16f, 0f, false, false, 16f, 80f); lineTo(16f, 176f); arcTo(16f, 16f, 0f, false, false, 32f, 192f); lineTo(200f, 192f); arcTo(16f, 16f, 0f, false, false, 216f, 176f); lineTo(216f, 80f); arcTo(16f, 16f, 0f, false, false, 200f, 64f); close()
                moveTo(124f, 100f); lineTo(100f, 132f); lineTo(132f, 132f); lineTo(108f, 164f)
            }
        }.build()

    // HardDrive (Storage - authentic Phosphor database / drive stacks)
    val HardDrive: ImageVector
        get() = ImageVector.Builder(
            name = "HardDrive",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).addPath(
            pathData = addPathNodes("M128,24C74.17,24,32,48.6,32,80v96c0,31.4,42.17,56,96,56s96-24.6,96-56V80C224,48.6,181.83,24,128,24Zm80,104c0,9.62-7.88,19.43-21.61,26.92C170.93,163.35,150.19,168,128,168s-42.93-4.65-58.39-13.08C55.88,147.43,48,137.62,48,128V111.36c17.06,15,46.23,24.64,80,24.64s62.94-9.68,80-24.64ZM69.61,53.08C85.07,44.65,105.81,40,128,40s42.93,4.65,58.39,13.08C200.12,60.57,208,70.38,208,80s-7.88,19.43-21.61,26.92C170.93,115.35,150.19,120,128,120s-42.93-4.65-58.39-13.08C55.88,99.43,48,89.62,48,80S55.88,60.57,69.61,53.08ZM186.39,202.92C170.93,211.35,150.19,216,128,216s-42.93-4.65-58.39-13.08C55.88,195.43,48,185.62,48,176V159.36c17.06,15,46.23,24.64,80,24.64s62.94-9.68,80-24.64V176C208,185.62,200.12,195.43,186.39,202.92Z"),
            fill = SolidColor(Color.Black)
        ).build()

    // Vpn (Shield & Lock - authentic Phosphor)
    val Vpn: ImageVector
        get() = ImageVector.Builder(
            name = "Vpn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).addPath(
            pathData = addPathNodes("M208,40H48A16,16 0 0,0 32,56v56c0,86.48 83.2,129.74 92.4,134.34a8,8 0 0,0 7.2,0C140.8,241.74 224,198.48 224,112V56A16,16 0 0,0 208,40ZM208,112c0,74.52 -70.47,113.88 -80,119C118.47,225.88 48,186.52 48,112V56H208Z"),
            fill = SolidColor(Color.Black)
        ).addPath(
            pathData = addPathNodes("M128,88a24,24 0 0,0 -24,24v8H96a8,8 0 0,0 -8,8v40a8,8 0 0,0 8,8h64a8,8 0 0,0 8,-8V128a8,8 0 0,0 -8,-8h-8v-8A24,24 0 0,0 128,88Zm16,32H112v-8a16,16 0 0,1 32,0Z"),
            fill = SolidColor(Color.Black)
        ).build()

    // ShieldCheck (Security & Privacy)
    val ShieldCheck: ImageVector
        get() = ImageVector.Builder(
            name = "ShieldCheck",
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
                moveTo(40f, 112f); curveTo(40f, 176f, 128f, 224f, 128f, 224f); curveTo(128f, 224f, 216f, 176f, 216f, 112f); lineTo(216f, 48f); lineTo(128f, 32f); lineTo(40f, 48f); close()
                moveTo(96f, 124f); lineTo(120f, 148f); lineTo(168f, 100f)
            }
        }.build()

    // ArrowsClockwise (System Update / Arrow counter-clockwise curved - authentic Phosphor)
    val ArrowsClockwise: ImageVector
        get() = ImageVector.Builder(
            name = "ArrowsClockwise",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).addPath(
            pathData = addPathNodes("M240,56v48a8,8,0,0,1-8,8H184a8,8,0,0,1,0-16H211.4L184.81,71.64l-.25-.24a80,80,0,1,0-1.67,114.78,8,8,0,0,1,11,11.63A95.44,95.44,0,0,1,128,224h-1.32A96,96,0,1,1,195.75,60L224,85.8V56a8,8,0,1,1,16,0Z"),
            fill = SolidColor(Color.Black)
        ).build()

    // GearSix (System - authentic Phosphor gear with internal hub)
    val GearSix: ImageVector
        get() = ImageVector.Builder(
            name = "GearSix",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).addPath(
            pathData = addPathNodes("M128,80a48,48,0,1,0,48,48A48.05,48.05,0,0,0,128,80Zm0,80a32,32,0,1,1,32-32A32,32,0,0,1,128,160Zm109.94-52.79a8,8,0,0,0-3.89-5.4l-29.83-17-.12-33.62a8,8,0,0,0-2.83-6.08,111.91,111.91,0,0,0-36.72-20.67,8,8,0,0,0-6.46.59L128,41.85,97.88,25a8,8,0,0,0-6.47-.6A112.1,112.1,0,0,0,54.73,45.15a8,8,0,0,0-2.83,6.07l-.15,33.65-29.83,17a8,8,0,0,0-3.89,5.4,106.47,106.47,0,0,0,0,41.56,8,8,0,0,0,3.89,5.4l29.83,17,.12,33.62a8,8,0,0,0,2.83,6.08,111.91,111.91,0,0,0,36.72,20.67,8,8,0,0,0,6.46-.59L128,214.15,158.12,231a7.91,7.91,0,0,0,3.9,1,8.09,8.09,0,0,0,2.57-.42,112.1,112.1,0,0,0,36.68-20.73,8,8,0,0,0,2.83-6.07l.15-33.65,29.83-17a8,8,0,0,0,3.89-5.4A106.47,106.47,0,0,0,237.94,107.21Zm-15,34.91-28.57,16.25a8,8,0,0,0-3,3c-.58,1-1.19,2.06-1.81,3.06a7.94,7.94,0,0,0-1.22,4.21l-.15,32.25a95.89,95.89,0,0,1-25.37,14.3L134,199.13a8,8,0,0,0-3.91-1h-.19c-1.21,0-2.43,0-3.64,0a8.08,8.08,0,0,0-4.1,1l-28.84,16.1A96,96,0,0,1,67.88,201l-.11-32.2a8,8,0,0,0-1.22-4.22c-.62-1-1.23-2-1.8-3.06a8.09,8.09,0,0,0-3-3.06l-28.6-16.29a90.49,90.49,0,0,1,0-28.26L61.67,97.63a8,8,0,0,0,3-3c.58-1,1.19-2.06,1.81-3.06a7.94,7.94,0,0,0,1.22-4.21l.15-32.25a95.89,95.89,0,0,1,25.37-14.3L122,56.87a8,8,0,0,0,4.1,1c1.21,0,2.43,0,3.64,0a8.08,8.08,0,0,0,4.1-1l28.84-16.1A96,96,0,0,1,188.12,55l.11,32.2a8,8,0,0,0,1.22,4.22c.62,1,1.23,2,1.8,3.06a8.09,8.09,0,0,0,3,3.06l28.6,16.29A90.49,90.49,0,0,1,222.9,142.12Z"),
            fill = SolidColor(Color.Black)
        ).build()

    // Info (About Phone)
    val Info: ImageVector
        get() = ImageVector.Builder(
            name = "Info",
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
                moveTo(128f, 24f); arcTo(104f, 104f, 0f, true, true, 24f, 128f); arcTo(104f, 104f, 0f, false, true, 128f, 24f); close()
                moveTo(120f, 120f); lineTo(128f, 120f); lineTo(128f, 176f); lineTo(136f, 176f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(124f, 84f); arcTo(8f, 8f, 0f, true, true, 132f, 92f); arcTo(8f, 8f, 0f, false, true, 124f, 84f); close()
            }
        }.build()

    // CaretRight
    val CaretRight: ImageVector
        get() = ImageVector.Builder(
            name = "CaretRight",
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
                moveTo(96f, 208f); lineTo(176f, 128f); lineTo(96f, 48f)
            }
        }.build()

    // Check
    val Check: ImageVector
        get() = ImageVector.Builder(
            name = "Check",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 20f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(48f, 136f); lineTo(96f, 184f); lineTo(208f, 72f)
            }
        }.build()

    // DotsThreeVertical
    val DotsThreeVertical: ImageVector
        get() = ImageVector.Builder(
            name = "DotsThreeVertical",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(128f, 60f); arcTo(12f, 12f, 0f, true, true, 128f, 36f); arcTo(12f, 12f, 0f, false, true, 128f, 60f); close()
                moveTo(128f, 140f); arcTo(12f, 12f, 0f, true, true, 128f, 116f); arcTo(12f, 12f, 0f, false, true, 128f, 140f); close()
                moveTo(128f, 220f); arcTo(12f, 12f, 0f, true, true, 128f, 196f); arcTo(12f, 12f, 0f, false, true, 128f, 220f); close()
            }
        }.build()

    // Trash (Delete)
    val Trash: ImageVector
        get() = ImageVector.Builder(
            name = "Trash",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).addPath(
            pathData = addPathNodes("M216,48H176V40a24,24,0,0,0-24-24H104A24,24,0,0,0,80,40v8H40a8,8,0,0,0,0,16h8V208a16,16,0,0,0,16,16H192a16,16,0,0,0,16-16V64h8a8,8,0,0,0,0-16ZM96,40a8,8,0,0,1,8-8h48a8,8,0,0,1,8,8v8H96Zm96,168H64V64H192ZM112,104v64a8,8,0,0,1-16,0V104a8,8,0,0,1,16,0Zm48,0v64a8,8,0,0,1-16,0V104a8,8,0,0,1,16,0Z"),
            fill = SolidColor(Color.Black)
        ).build()

    // Phone
    val Phone: ImageVector
        get() = ImageVector.Builder(
            name = "Phone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(222.37f, 158.46f); lineTo(174f, 137.74f)
                arcTo(16f, 16f, 0f, false, false, 155.6f, 143.51f)
                lineTo(139.77f, 159.34f)
                arcTo(160.71f, 160.71f, 0f, false, true, 96.66f, 116.23f)
                lineTo(112.49f, 100.4f)
                arcTo(16f, 16f, 0f, false, false, 118.26f, 82f)
                lineTo(97.54f, 33.63f)
                arcTo(16f, 16f, 0f, false, false, 79.59f, 24f)
                horizontalLineTo(40f)
                arcTo(16f, 16f, 0f, false, false, 24f, 40f)
                arcTo(192.21f, 192.21f, 0f, false, false, 216f, 232f)
                arcTo(16f, 16f, 0f, false, false, 232f, 216f)
                verticalLineTo(176.41f)
                arcTo(16f, 16f, 0f, false, false, 222.37f, 158.46f)
                close()
            }
        }.build()

    // ChatCircleDots (Messages)
    val ChatCircleDots: ImageVector
        get() = ImageVector.Builder(
            name = "ChatCircleDots",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(128f, 24f)
                arcTo(104f, 104f, 0f, false, false, 36.18f, 176.88f)
                lineTo(24.83f, 218.35f)
                arcTo(16f, 16f, 0f, false, false, 37.65f, 231.17f)
                lineTo(79.12f, 219.82f)
                arcTo(104f, 104f, 0f, true, false, 128f, 24f)
                close()
            }
        }.build()

    // Camera
    val Camera: ImageVector
        get() = ImageVector.Builder(
            name = "Camera",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(208f, 56f); horizontalLineTo(180.28f); lineTo(166.4f, 35.2f)
                arcTo(16f, 16f, 0f, false, false, 153.07f, 24f); horizontalLineTo(102.93f)
                arcTo(16f, 16f, 0f, false, false, 89.6f, 35.2f); lineTo(75.72f, 56f)
                horizontalLineTo(48f); arcTo(24f, 24f, 0f, false, false, 24f, 80f)
                verticalLineTo(192f); arcTo(24f, 24f, 0f, false, false, 48f, 216f)
                horizontalLineTo(208f); arcTo(24f, 24f, 0f, false, false, 232f, 192f)
                verticalLineTo(80f); arcTo(24f, 24f, 0f, false, false, 208f, 56f)
                close()
                moveTo(128f, 176f); arcTo(40f, 40f, 0f, true, true, 168f, 136f)
                arcTo(40f, 40f, 0f, false, true, 128f, 176f)
                close()
            }
        }.build()

    // Image (Gallery)
    val Image: ImageVector
        get() = ImageVector.Builder(
            name = "Image",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(216f, 40f); horizontalLineTo(40f); arcTo(16f, 16f, 0f, false, false, 24f, 56f)
                verticalLineTo(200f); arcTo(16f, 16f, 0f, false, false, 40f, 216f); horizontalLineTo(216f)
                arcTo(16f, 16f, 0f, false, false, 232f, 200f); verticalLineTo(56f)
                arcTo(16f, 16f, 0f, false, false, 216f, 40f)
                close()
                moveTo(216f, 178.75f); lineTo(162.75f, 125.5f); arcTo(16f, 16f, 0f, false, false, 140.12f, 125.5f)
                lineTo(72f, 193.62f); verticalLineTo(56f); horizontalLineTo(216f)
                close()
            }
        }.build()

    // Globe (Browser / Chrome)
    val Globe: ImageVector
        get() = ImageVector.Builder(
            name = "Globe",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(128f, 24f); arcTo(104f, 104f, 0f, true, false, 232f, 128f)
                arcTo(104.11f, 104.11f, 0f, false, false, 128f, 24f)
                close()
                moveTo(128f, 40.85f); arcTo(174.62f, 174.62f, 0f, false, true, 162.58f, 120f)
                horizontalLineTo(93.42f); arcTo(174.62f, 174.62f, 0f, false, true, 128f, 40.85f)
                close()
            }
        }.build()

    // Clock
    val Clock: ImageVector
        get() = ImageVector.Builder(
            name = "Clock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(128f, 24f); arcTo(104f, 104f, 0f, true, false, 232f, 128f)
                arcTo(104.11f, 104.11f, 0f, false, false, 128f, 24f)
                close()
                moveTo(165.66f, 162.34f); lineTo(120f, 136f); verticalLineTo(72f)
                arcTo(8f, 8f, 0f, false, true, 136f, 72f); verticalLineTo(126.66f)
                lineTo(174.34f, 147.66f); arcTo(8f, 8f, 0f, false, true, 165.66f, 162.34f)
                close()
            }
        }.build()

    // Calendar
    val Calendar: ImageVector
        get() = ImageVector.Builder(
            name = "Calendar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(208f, 32f); horizontalLineTo(184f); verticalLineTo(24f)
                arcTo(8f, 8f, 0f, false, false, 168f, 24f); verticalLineTo(32f)
                horizontalLineTo(88f); verticalLineTo(24f); arcTo(8f, 8f, 0f, false, false, 72f, 24f)
                verticalLineTo(32f); horizontalLineTo(48f); arcTo(16f, 16f, 0f, false, false, 32f, 48f)
                verticalLineTo(208f); arcTo(16f, 16f, 0f, false, false, 48f, 224f); horizontalLineTo(208f)
                arcTo(16f, 16f, 0f, false, false, 224f, 208f); verticalLineTo(48f)
                arcTo(16f, 16f, 0f, false, false, 208f, 32f)
                close()
                moveTo(208f, 80f); horizontalLineTo(48f); verticalLineTo(48f); horizontalLineTo(72f)
                verticalLineTo(56f); arcTo(8f, 8f, 0f, false, false, 88f, 56f); verticalLineTo(48f)
                horizontalLineTo(168f); verticalLineTo(56f); arcTo(8f, 8f, 0f, false, false, 184f, 56f)
                verticalLineTo(48f); horizontalLineTo(208f)
                close()
            }
        }.build()

    // Folder
    val Folder: ImageVector
        get() = ImageVector.Builder(
            name = "Folder",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(216f, 72f); horizontalLineTo(130.67f); lineTo(102.93f, 44.27f)
                arcTo(16.12f, 16.12f, 0f, false, false, 91.6f, 40f); horizontalLineTo(40f)
                arcTo(16f, 16f, 0f, false, false, 24f, 56f); verticalLineTo(200f)
                arcTo(16f, 16f, 0f, false, false, 40f, 216f); horizontalLineTo(216f)
                arcTo(16f, 16f, 0f, false, false, 232f, 200f); verticalLineTo(88f)
                arcTo(16f, 16f, 0f, false, false, 216f, 72f)
                close()
            }
        }.build()

    // Sparkle (Clean star)
    val Sparkle: ImageVector
        get() = ImageVector.Builder(
            name = "Sparkle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(218.83f, 115.86f); lineTo(151.7f, 96.3f); lineTo(132.14f, 29.17f)
                arcTo(8f, 8f, 0f, false, false, 116.86f, 29.17f); lineTo(97.3f, 96.3f)
                lineTo(30.17f, 115.86f); arcTo(8f, 8f, 0f, false, false, 30.17f, 131.14f)
                lineTo(97.3f, 150.7f); lineTo(116.86f, 217.83f); arcTo(8f, 8f, 0f, false, false, 132.14f, 217.83f)
                lineTo(151.7f, 150.7f); lineTo(218.83f, 131.14f); arcTo(8f, 8f, 0f, false, false, 218.83f, 115.86f)
                close()
            }
        }.build()
}
