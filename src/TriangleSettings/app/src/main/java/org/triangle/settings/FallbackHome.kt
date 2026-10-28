package org.triangle.settings

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserManager
import android.util.Log
import android.view.Gravity
import android.widget.TextView

/**
 * Прямой аналог системного Settings.FallbackHome, который мы потеряли, заменив
 * com.android.settings своим пакетом.
 *
 * Зачем он нужен:
 *   При загрузке пользователь ещё RUNNING_LOCKED, поэтому PackageManager
 *   резолвит только directBootAware-компоненты. Настоящий лаунчер таким не
 *   является, а прежний FallbackHome пропал вместе с системными Settings.
 *   WMS не находит home-activity, ни одна activity не становится idle,
 *   ActivityTaskSupervisor не вызывает checkFinishBootingLocked(), boot не
 *   завершается: mSystemBooted=false, DispatchEnabled=false. Экран блокировки
 *   при этом нарисован, но не реагирует ни на касания, ни на кнопки.
 *
 * Что делает эта activity:
 *   Показывается как домашний экран, пока пользователь заблокирован. Как
 *   только пользователь разблокирован и появляется настоящий home, activity
 *   завершается — система сама поднимает лаунчер.
 */
class FallbackHome : Activity() {

    private val handler = Handler(Looper.getMainLooper())

    private val checkUnlocked = object : Runnable {
        override fun run() {
            if (maybeFinish()) return
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        setContentView(TextView(this).apply {
            text = "Triangle OS"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 22f
        })
        Log.i(TAG, "FallbackHome shown, waiting for user unlock")
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(checkUnlocked)
        handler.post(checkUnlocked)
    }

    override fun onPause() {
        handler.removeCallbacks(checkUnlocked)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkUnlocked)
        super.onDestroy()
    }

    /**
     * @return true, если activity завершилась и опрос больше не нужен.
     */
    private fun maybeFinish(): Boolean {
        val userManager = getSystemService(UserManager::class.java) ?: return false
        if (!userManager.isUserUnlocked) return false

        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val resolvedPackage = resolved?.activityInfo?.packageName

        if (resolvedPackage == null || resolvedPackage == packageName) {
            // Настоящего домашнего экрана ещё нет — продолжаем ждать.
            return false
        }

        Log.i(TAG, "User unlocked and real home found ($resolvedPackage); leaving")
        finish()
        return true
    }

    private companion object {
        const val TAG = "TriangleFallbackHome"
        const val CHECK_INTERVAL_MS = 500L
    }
}
