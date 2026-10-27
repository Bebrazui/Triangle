package org.triangle.launcher.model

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Конфигурация интерфейса лаунчера с автосохранением в SharedPreferences.
 * Изменения применяются на лету в реальном времени.
 */
object LauncherConfig {
    private const val PREFS_NAME = "triangle_launcher_settings"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        desktopIconSizeFloat = prefs.getFloat("desktop_icon_size", 60f)
        dockIconSizeFloat = prefs.getFloat("dock_icon_size", 60f)
        iconCornerRadiusFloat = prefs.getFloat("icon_corner_radius", 18f)
        springStiffnessOpening = prefs.getFloat("spring_stiffness_opening", 500f)
        springDampingOpening = prefs.getFloat("spring_damping_opening", 1.10f)
        springStiffnessClosing = prefs.getFloat("spring_stiffness_closing", 550f)
        springDampingClosing = prefs.getFloat("spring_damping_closing", 1.04f)
    }

    // 1. РАЗМЕРЫ ИКОНОК
    var desktopIconSizeFloat by mutableFloatStateOf(60f)
        private set
    val desktopIconSize: Dp get() = desktopIconSizeFloat.dp

    var dockIconSizeFloat by mutableFloatStateOf(60f)
        private set
    val dockIconSize: Dp get() = dockIconSizeFloat.dp

    var iconCornerRadiusFloat by mutableFloatStateOf(18f)
        private set
    val iconCornerRadiusDp: Dp get() = iconCornerRadiusFloat.dp

    val drawerIconSize: Dp = 44.dp
    val iconSlotHeight: Dp get() = (desktopIconSizeFloat + 26f).dp

    // 2. ФИЗИКА И СКОРОСТЬ АНИМАЦИИ
    var springStiffnessOpening by mutableFloatStateOf(500f)
        private set
    var springDampingOpening by mutableFloatStateOf(1.10f)
        private set

    var springStiffnessClosing by mutableFloatStateOf(550f)
        private set
    var springDampingClosing by mutableFloatStateOf(1.04f)
        private set

    val springRadiusStiffness: Float = 950f
    val springAppAlphaStiffness: Float = 1000f

    fun updateDesktopIconSize(size: Float) {
        desktopIconSizeFloat = size
        if (::prefs.isInitialized) prefs.edit().putFloat("desktop_icon_size", size).apply()
    }

    fun updateDockIconSize(size: Float) {
        dockIconSizeFloat = size
        if (::prefs.isInitialized) prefs.edit().putFloat("dock_icon_size", size).apply()
    }

    fun updateIconCornerRadius(radius: Float) {
        iconCornerRadiusFloat = radius
        if (::prefs.isInitialized) prefs.edit().putFloat("icon_corner_radius", radius).apply()
    }

    fun updateOpeningPhysics(stiffness: Float, damping: Float) {
        springStiffnessOpening = stiffness
        springDampingOpening = damping
        if (::prefs.isInitialized) {
            prefs.edit()
                .putFloat("spring_stiffness_opening", stiffness)
                .putFloat("spring_damping_opening", damping)
                .apply()
        }
    }

    fun updateClosingPhysics(stiffness: Float, damping: Float) {
        springStiffnessClosing = stiffness
        springDampingClosing = damping
        if (::prefs.isInitialized) {
            prefs.edit()
                .putFloat("spring_stiffness_closing", stiffness)
                .putFloat("spring_damping_closing", damping)
                .apply()
        }
    }
}
