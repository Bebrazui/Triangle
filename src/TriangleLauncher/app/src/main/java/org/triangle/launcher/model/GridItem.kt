package org.triangle.launcher.model

enum class IconShape(val cornerRadiusPercent: Int) {
    SQUIRCLE(32),
    ROUNDED(24),
    CIRCLE(50),
    SQUARE(12)
}

enum class ItemSpan(val spanX: Int, val spanY: Int) {
    SPAN_1x1(1, 1),
    SPAN_2x1(2, 1),
    SPAN_2x2(2, 2)
}

data class GridItem(
    val id: String,
    val packageName: String,
    val cellX: Int,
    val cellY: Int,
    val span: ItemSpan = ItemSpan.SPAN_1x1,
    val shape: IconShape = IconShape.SQUIRCLE
)
