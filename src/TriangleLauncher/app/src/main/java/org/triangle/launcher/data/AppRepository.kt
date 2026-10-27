package org.triangle.launcher.data

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.triangle.launcher.model.AppModel
import org.triangle.launcher.util.ColorExtractor

class AppRepository(private val context: Context) {

    private val _apps = MutableStateFlow<List<AppModel>>(emptyList())
    val apps: StateFlow<List<AppModel>> = _apps.asStateFlow()

    private val _wallpaperBitmap = MutableStateFlow<Bitmap?>(null)
    val wallpaperBitmap: StateFlow<Bitmap?> = _wallpaperBitmap.asStateFlow()

    private val packageManager: PackageManager = context.packageManager
    private val scope = CoroutineScope(Dispatchers.IO)

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            reloadApps()
        }
    }

    init {
        registerPackageReceiver()
        reloadApps()
        loadSystemWallpaper()
    }

    fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        context.registerReceiver(packageReceiver, filter)
    }

    fun unregisterPackageReceiver() {
        try {
            context.unregisterReceiver(packageReceiver)
        } catch (_: Exception) {}
    }

    fun reloadApps() {
        scope.launch {
            val appList = loadInstalledApps()
            _apps.value = appList.sortedBy { it.label.lowercase() }
        }
    }

    private fun loadSystemWallpaper() {
        scope.launch {
            try {
                val wm = WallpaperManager.getInstance(context)
                val drawable = wm.drawable
                if (drawable != null) {
                    _wallpaperBitmap.value = drawableToBitmap(drawable)
                }
            } catch (_: Exception) {}
        }
    }

    private suspend fun loadInstalledApps(): List<AppModel> = withContext(Dispatchers.IO) {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
        val userHandle: UserHandle = Process.myUserHandle()

        val rawList = mutableListOf<AppModel>()

        if (launcherApps != null) {
            val activities = launcherApps.getActivityList(null, userHandle)
            for (info in activities) {
                val pkg = info.applicationInfo.packageName
                if (pkg == context.packageName) continue
                val label = info.label.toString()
                val iconDrawable = info.getBadgedIcon(0)
                val bitmap = drawableToBitmap(iconDrawable)
                val dominantColor = ColorExtractor.extractDominantColor(bitmap)
                rawList.add(
                    AppModel(
                        packageName = pkg,
                        activityName = info.name,
                        label = label,
                        icon = bitmap,
                        dominantColor = dominantColor
                    )
                )
            }
        }

        // Fallback or augment via PackageManager to guarantee core system apps (Dialer, Messaging, etc.) are always present
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        for (ri in resolveInfos) {
            val pkg = ri.activityInfo.packageName
            if (pkg == context.packageName) continue
            if (rawList.none { it.packageName == pkg }) {
                val label = ri.loadLabel(packageManager).toString()
                val iconDrawable = ri.loadIcon(packageManager)
                val bitmap = drawableToBitmap(iconDrawable)
                val dominantColor = ColorExtractor.extractDominantColor(bitmap)
                rawList.add(
                    AppModel(
                        packageName = pkg,
                        activityName = ri.activityInfo.name,
                        label = label,
                        icon = bitmap,
                        dominantColor = dominantColor
                    )
                )
            }
        }

        return@withContext rawList
    }

    fun launchApp(app: AppModel) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(app.packageName, app.activityName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = packageManager.getLaunchIntentForPackage(app.packageName)
            fallback?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
        }
    }

    fun openAppInfo(app: AppModel) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${app.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 144
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 144
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
