package org.triangle.launcher

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.triangle.launcher.data.AppRepository
import org.triangle.launcher.ui.screens.LauncherHomeScreen
import org.triangle.launcher.ui.theme.TriangleLauncherTheme

class MainActivity : ComponentActivity() {

    private lateinit var appRepository: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        org.triangle.launcher.model.LauncherConfig.init(this)
        appRepository = AppRepository(this)

        setContent {
            TriangleLauncherTheme {
                LauncherHomeScreen(
                    appRepository = appRepository,
                    onOpenSettings = {
                        try {
                            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(intent)
                        } catch (_: Exception) {}
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::appRepository.isInitialized) {
            appRepository.unregisterPackageReceiver()
        }
    }
}
