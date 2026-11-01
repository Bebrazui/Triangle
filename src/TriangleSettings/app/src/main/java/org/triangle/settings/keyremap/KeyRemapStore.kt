package org.triangle.settings.keyremap

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager

/**
 * Хранилище конфига переназначения.
 *
 * Источник истины — Settings.Secure, а не SharedPreferences: этот же конфиг
 * в будущем должен читать системный компонент (SystemUI / framework), у
 * которого нет доступа к приватным файлам приложения.
 */
object KeyRemapStore {

    const val SECURE_KEY = "triangle_key_remap_v1"
    private const val TAG = "KeyRemap"

    sealed class SaveResult {
        object Ok : SaveResult()
        data class Failed(val reason: String) : SaveResult()
    }

    /** Последняя ошибка чтения — экран показывает её вместо выдуманного «всё хорошо». */
    @Volatile
    var lastLoadError: String? = null
        private set

    @Volatile
    var lastSaveError: String? = null
        private set

    fun load(context: Context): KeyRemapConfig {
        val raw = try {
            Settings.Secure.getString(context.contentResolver, SECURE_KEY)
        } catch (e: Exception) {
            lastLoadError = "Не удалось прочитать настройку: ${e.message}"
            Log.w(TAG, "load failed", e)
            return KeyRemapConfig()
        }
        val decoded = KeyRemapCodec.decode(raw)
        lastLoadError = if (decoded == null) {
            "Конфиг повреждён, привязки отключены. Сбросьте их и настройте заново."
        } else {
            null
        }
        return decoded ?: KeyRemapConfig(bindings = emptyList())
    }

    fun save(context: Context, config: KeyRemapConfig): SaveResult {
        val clean = config.sanitized()
        val encoded = KeyRemapCodec.encode(clean)
        return try {
            val ok = Settings.Secure.putString(context.contentResolver, SECURE_KEY, encoded)
            if (!ok) {
                lastSaveError = "Система отклонила запись Settings.Secure"
                return SaveResult.Failed(lastSaveError!!)
            }
            // Проверка обратным чтением: молча «сохранённое» — не сохранённое.
            val readBack = Settings.Secure.getString(context.contentResolver, SECURE_KEY)
            if (readBack != encoded) {
                lastSaveError = "Запись не подтвердилась обратным чтением"
                return SaveResult.Failed(lastSaveError!!)
            }
            lastSaveError = null
            SaveResult.Ok
        } catch (e: Exception) {
            lastSaveError = e.message ?: e.javaClass.simpleName
            Log.w(TAG, "save failed", e)
            SaveResult.Failed(lastSaveError!!)
        }
    }

    fun clear(context: Context): SaveResult {
        return try {
            Settings.Secure.putString(context.contentResolver, SECURE_KEY, null)
            lastSaveError = null
            SaveResult.Ok
        } catch (e: Exception) {
            lastSaveError = e.message ?: e.javaClass.simpleName
            SaveResult.Failed(lastSaveError!!)
        }
    }

    /** Уведомляет об изменении конфига извне (adb, другой компонент системы). */
    fun registerObserver(context: Context, onChange: () -> Unit): ContentObserver {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                onChange()
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(SECURE_KEY),
            false,
            observer
        )
        return observer
    }

    fun serviceComponent(context: Context): ComponentName =
        ComponentName(context.packageName, KeyRemapService::class.java.name)

    fun serviceComponentString(context: Context): String =
        serviceComponent(context).flattenToShortString()

    /**
     * Реальное состояние службы по данным AccessibilityManager.
     * Если система ещё не применила запись — вернётся false, и интерфейс
     * честно покажет «не включено», а не «включено».
     */
    fun isServiceEnabled(context: Context): Boolean {
        val expected = serviceComponent(context)
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        if (am != null) {
            val enabled = try {
                am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            } catch (e: Exception) {
                emptyList()
            }
            if (enabled.any { it.resolveInfo?.serviceInfo?.let { si -> ComponentName(si.packageName, si.name) == expected } == true }) {
                return true
            }
        }
        // Резервная проверка по secure-настройке: нужна, когда менеджер ещё не
        // перечитал состояние, но запись уже применилась.
        return enabledComponentsFromSettings(context).contains(expected.flattenToShortString())
    }

    fun enabledComponentsFromSettings(context: Context): List<String> {
        val raw = try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } catch (e: Exception) {
            null
        } ?: return emptyList()
        return raw.split(':').map { it.trim() }.filter { it.isNotEmpty() }
    }

    /**
     * Включает/выключает службу через системную настройку. Требует
     * WRITE_SECURE_SETTINGS (у платформенного Settings он есть).
     * Возвращает результат записи; фактическое подключение проверяется
     * отдельно через [isServiceEnabled] и [KeyRemapService.isConnected].
     */
    fun setServiceEnabled(context: Context, enable: Boolean): SaveResult {
        return try {
            val target = serviceComponentString(context)
            val current = enabledComponentsFromSettings(context).toMutableList()
            val alreadyThere = current.any { normalize(it) == normalize(target) }
            if (enable && !alreadyThere) current.add(target)
            if (!enable) current.removeAll { normalize(it) == normalize(target) }

            Settings.Secure.putString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                current.joinToString(":")
            )
            Settings.Secure.putInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                if (current.isEmpty()) 0 else 1
            )
            SaveResult.Ok
        } catch (e: Exception) {
            Log.w(TAG, "setServiceEnabled failed", e)
            SaveResult.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun normalize(component: String): String = component.trim().lowercase()
}
