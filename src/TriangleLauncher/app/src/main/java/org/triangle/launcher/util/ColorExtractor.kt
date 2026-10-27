package org.triangle.launcher.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils

object ColorExtractor {
    fun extractDominantColor(bitmap: Bitmap?): Color {
        if (bitmap == null) return Color(0xFF2C3E50)
        
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0L

        // Семплируем пиксели для быстрой и точной средней доминанты
        val stepX = (bitmap.width / 16).coerceAtLeast(1)
        val stepY = (bitmap.height / 16).coerceAtLeast(1)

        for (x in 0 until bitmap.width step stepX) {
            for (y in 0 until bitmap.height step stepY) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha > 128) {
                    val r = (pixel ushr 16) and 0xFF
                    val g = (pixel ushr 8) and 0xFF
                    val b = pixel and 0xFF
                    
                    // Исключаем чисто белый и чисто черный шум
                    val brightness = (r + g + b) / 3
                    if (brightness in 30..235) {
                        rSum += r
                        gSum += g
                        bSum += b
                        count++
                    }
                }
            }
        }

        if (count == 0L) return Color(0xFF334155)

        val avgR = (rSum / count).toInt()
        val avgG = (gSum / count).toInt()
        val avgB = (bSum / count).toInt()

        return Color(android.graphics.Color.rgb(avgR, avgG, avgB))
    }
}
