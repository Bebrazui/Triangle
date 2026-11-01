package org.triangle.settings.keyremap

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.database.ContentObserver
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Служба перехвата клавиш.
 *
 * Единственная задача — отдать события в [KeyRemapEngine] и вернуть решение
 * «поглотить или пропустить». Ничего не читает с экрана и не запрашивает
 * доступ к содержимому окон.
 */
class KeyRemapService : AccessibilityService() {

    private var observer: ContentObserver? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        connected = true
        KeyRemapEngine.attach(this, this)
        observer = KeyRemapStore.registerObserver(this) {
            KeyRemapEngine.reload(this)
        }
        Log.i(TAG, "service connected")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        return try {
            KeyRemapEngine.handle(event, this)
        } catch (e: Exception) {
            // Сбой логики не должен блокировать клавиатуру устройства.
            Log.e(TAG, "onKeyEvent failed", e)
            KeyRemapEngine.reset()
            false
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Намеренно пусто: события окон этой службе не нужны.
    }

    override fun onInterrupt() {
        KeyRemapEngine.reset()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        connected = false
        observer?.let {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                Log.w(TAG, "observer unregister failed", e)
            }
        }
        observer = null
        KeyRemapEngine.detach()
        Log.i(TAG, "service disconnected")
    }

    companion object {
        private const val TAG = "KeyRemap"

        @Volatile
        var connected: Boolean = false
            private set

        fun isConnected(): Boolean = connected
    }
}
