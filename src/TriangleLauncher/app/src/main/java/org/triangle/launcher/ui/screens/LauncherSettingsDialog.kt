package org.triangle.launcher.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.triangle.launcher.model.LauncherConfig
import org.triangle.launcher.ui.icons.Phosphor
import kotlin.math.roundToInt

@Composable
fun LauncherSettingsDialog(
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        color = Color(0xFF141720),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 14.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Параметры Лаунчера",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Phosphor.X,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // РАЗДЕЛ: ИКОНКИ
                Text(
                    text = "ВНЕШНИЙ ВИД И ИКОНКИ",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f)
                )

                SettingSliderItem(
                    title = "Размер иконок на столе",
                    value = LauncherConfig.desktopIconSizeFloat,
                    valueRange = 48f..76f,
                    valueLabel = "${LauncherConfig.desktopIconSizeFloat.roundToInt()} dp",
                    onValueChange = { LauncherConfig.updateDesktopIconSize(it) }
                )

                SettingSliderItem(
                    title = "Размер иконок в доке",
                    value = LauncherConfig.dockIconSizeFloat,
                    valueRange = 48f..76f,
                    valueLabel = "${LauncherConfig.dockIconSizeFloat.roundToInt()} dp",
                    onValueChange = { LauncherConfig.updateDockIconSize(it) }
                )

                SettingSliderItem(
                    title = "Скругление углов иконки",
                    value = LauncherConfig.iconCornerRadiusFloat,
                    valueRange = 0f..30f,
                    valueLabel = "${LauncherConfig.iconCornerRadiusFloat.roundToInt()} dp",
                    onValueChange = { LauncherConfig.updateIconCornerRadius(it) }
                )

                Divider(color = Color.White.copy(alpha = 0.1f))

                // РАЗДЕЛ: АНИМАЦИИ
                Text(
                    text = "СКОРОСТЬ И ПРУЖИНЫ АНИМАЦИИ (ORIGINOS)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f)
                )

                SettingSliderItem(
                    title = "Скорость открытия (Жесткость k)",
                    value = LauncherConfig.springStiffnessOpening,
                    valueRange = 250f..1000f,
                    valueLabel = "${LauncherConfig.springStiffnessOpening.roundToInt()}",
                    onValueChange = { LauncherConfig.updateOpeningPhysics(it, LauncherConfig.springDampingOpening) }
                )

                SettingSliderItem(
                    title = "Плавность открытия (Демпфирование d)",
                    value = LauncherConfig.springDampingOpening,
                    valueRange = 0.70f..1.40f,
                    valueLabel = String.format("%.2f", LauncherConfig.springDampingOpening),
                    onValueChange = { LauncherConfig.updateOpeningPhysics(LauncherConfig.springStiffnessOpening, it) }
                )

                SettingSliderItem(
                    title = "Скорость закрытия (Жесткость k)",
                    value = LauncherConfig.springStiffnessClosing,
                    valueRange = 250f..1000f,
                    valueLabel = "${LauncherConfig.springStiffnessClosing.roundToInt()}",
                    onValueChange = { LauncherConfig.updateClosingPhysics(it, LauncherConfig.springDampingClosing) }
                )

                SettingSliderItem(
                    title = "Плавность закрытия (Демпфирование d)",
                    value = LauncherConfig.springDampingClosing,
                    valueRange = 0.70f..1.40f,
                    valueLabel = String.format("%.2f", LauncherConfig.springDampingClosing),
                    onValueChange = { LauncherConfig.updateClosingPhysics(LauncherConfig.springStiffnessClosing, it) }
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SettingSliderItem(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White.copy(alpha = 0.9f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
        }
    }
}
