package org.triangle.launcher.model

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color

data class AppModel(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Bitmap? = null,
    val dominantColor: Color = Color(0xFF2C3E50)
)
