package org.triangle.settings.keyremap

import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.PowerManager
import android.view.KeyEvent
import android.accessibilityservice.AccessibilityService
import android.widget.Toast

sealed class ActionResult {
    object Ok : ActionResult()
    data class Error(val message: String) : ActionResult()
}

/**
 * Исполнение действия, выбранного в привязке.
 *
 * Ничего не инжектит в InputDispatcher: используются штатные точки —
 * AccessibilityService.performGlobalAction, AudioManager, startActivity.
 * Поэтому нет риска рекурсии (своё же нажатие снова не перехватывается)
 * и не нужно разрешение INJECT_EVENTS.
 */
class KeyActionExecutor(private val service: AccessibilityService) {

    fun execute(action: RemapAction): ActionResult = try {
        when (action.type) {
            RemapActionType.GLOBAL -> executeGlobal(action)
            RemapActionType.MEDIA -> executeMedia(action)
            RemapActionType.LAUNCH_APP -> executeLaunchApp(action)
            RemapActionType.INTENT -> executeIntent(action)
        }
    } catch (e: SecurityException) {
        ActionResult.Error("система отклонила действие: ${e.message ?: "SecurityException"}")
    } catch (e: Exception) {
        ActionResult.Error(e.message ?: e.javaClass.simpleName)
    }

    private fun executeGlobal(action: RemapAction): ActionResult {
        val target = GlobalAction.fromId(action.globalAction)
            ?: return ActionResult.Error("неизвестное системное действие")
        if (target == GlobalAction.NONE) return ActionResult.Ok
        val globalAction = when (target) {
            GlobalAction.BACK -> AccessibilityService.GLOBAL_ACTION_BACK
            GlobalAction.HOME -> AccessibilityService.GLOBAL_ACTION_HOME
            GlobalAction.RECENTS -> AccessibilityService.GLOBAL_ACTION_RECENTS
            GlobalAction.NOTIFICATIONS -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
            GlobalAction.QUICK_SETTINGS -> AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
            GlobalAction.SCREENSHOT -> AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
            GlobalAction.NONE -> return ActionResult.Ok
        }
        return if (service.performGlobalAction(globalAction)) {
            ActionResult.Ok
        } else {
            ActionResult.Error("система не выполнила действие «${target.label}»")
        }
    }

    private fun executeMedia(action: RemapAction): ActionResult {
        val target = MediaAction.fromId(action.mediaAction)
            ?: return ActionResult.Error("неизвестное медиа-действие")
        val audio = service.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionResult.Error("аудиослужба недоступна")
        val keyCode = when (target) {
            MediaAction.VOLUME_UP -> KeyEvent.KEYCODE_VOLUME_UP
            MediaAction.VOLUME_DOWN -> KeyEvent.KEYCODE_VOLUME_DOWN
            MediaAction.PLAY_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            MediaAction.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
            MediaAction.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            MediaAction.STOP -> KeyEvent.KEYCODE_MEDIA_STOP
        }
        return when (target) {
            MediaAction.VOLUME_UP, MediaAction.VOLUME_DOWN -> {
                val direction = if (target == MediaAction.VOLUME_UP) {
                    AudioManager.ADJUST_RAISE
                } else {
                    AudioManager.ADJUST_LOWER
                }
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
                ActionResult.Ok
            }
            else -> {
                // dispatchMediaKeyEvent не проходит через InputDispatcher,
                // поэтому повторного перехвата не будет.
                audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
                ActionResult.Ok
            }
        }
    }

    private fun executeLaunchApp(action: RemapAction): ActionResult {
        if (action.packageName.isBlank()) return ActionResult.Error("приложение не выбрано")
        val pm = service.packageManager
        val intent = try {
            pm.getLaunchIntentForPackage(action.packageName)
        } catch (e: Exception) {
            null
        } ?: return ActionResult.Error("у «${action.packageName}» нет запускаемой активности")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startSafely(intent)
    }

    private fun executeIntent(action: RemapAction): ActionResult {
        if (action.intentAction.isBlank() &&
            action.intentData.isBlank() &&
            action.packageName.isBlank() &&
            action.intentComponent.isBlank()
        ) {
            return ActionResult.Error("интент пуст: укажите action, data или компонент")
        }
        val intent = Intent()
        if (action.intentAction.isNotBlank()) intent.action = action.intentAction
        if (action.intentData.isNotBlank()) intent.data = Uri.parse(action.intentData)
        if (action.intentType.isNotBlank()) intent.type = action.intentType
        if (action.intentComponent.isNotBlank()) {
            val component = ComponentName.unflattenFromString(action.intentComponent)
                ?: return ActionResult.Error("неверный компонент: ${action.intentComponent}")
            intent.component = component
        } else if (action.packageName.isNotBlank()) {
            intent.setPackage(action.packageName)
        }
        intent.flags = action.intentFlags or Intent.FLAG_ACTIVITY_NEW_TASK

        for (extra in action.extras) {
            when (extra.type) {
                "string" -> intent.putExtra(extra.key, extra.value)
                "int" -> {
                    val value = extra.value.trim().toIntOrNull()
                        ?: return ActionResult.Error("«${extra.key}»: не число")
                    intent.putExtra(extra.key, value)
                }
                "long" -> {
                    val value = extra.value.trim().toLongOrNull()
                        ?: return ActionResult.Error("«${extra.key}»: не число")
                    intent.putExtra(extra.key, value)
                }
                "float" -> {
                    val value = extra.value.trim().toFloatOrNull()
                        ?: return ActionResult.Error("«${extra.key}»: не число")
                    intent.putExtra(extra.key, value)
                }
                "boolean" -> {
                    val value = extra.value.trim().lowercase()
                    if (value != "true" && value != "false") {
                        return ActionResult.Error("«${extra.key}»: ожидается true/false")
                    }
                    intent.putExtra(extra.key, value == "true")
                }
                "string_array" -> intent.putExtra(
                    extra.key,
                    extra.value.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toTypedArray()
                )
                "int_array" -> {
                    val values = extra.value.split(',').mapNotNull { it.trim().toIntOrNull() }
                    if (values.isEmpty() && extra.value.isNotBlank()) {
                        return ActionResult.Error("«${extra.key}»: ожидался список чисел")
                    }
                    intent.putExtra(extra.key, values.toIntArray())
                }
                else -> return ActionResult.Error("«${extra.key}»: неизвестный тип ${extra.type}")
            }
        }
        return startSafely(intent)
    }

    private fun startSafely(intent: Intent): ActionResult {
        return try {
            service.startActivity(intent)
            ActionResult.Ok
        } catch (e: android.content.ActivityNotFoundException) {
            ActionResult.Error("некому обработать интент")
        } catch (e: SecurityException) {
            ActionResult.Error("нет права запускать это: ${e.message ?: "SecurityException"}")
        }
    }

    /** Проверка, доступен ли запуск приложения (для интерфейса и журнала). */
    fun canLaunchPackage(packageName: String): Boolean = try {
        service.packageManager.getLaunchIntentForPackage(packageName) != null
    } catch (e: Exception) {
        false
    }

    fun isKeyguardLocked(): Boolean {
        val km = service.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return false
        return km.isKeyguardLocked
    }

    fun isInteractive(): Boolean {
        val pm = service.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isInteractive
    }

    fun notify(message: String) {
        try {
            Toast.makeText(service, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Toast может быть недоступен (редко); сообщение уже ушло в журнал.
        }
    }
}
