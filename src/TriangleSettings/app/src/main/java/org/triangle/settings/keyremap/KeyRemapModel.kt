package org.triangle.settings.keyremap

import android.content.Context
import android.view.KeyEvent
import org.json.JSONArray
import org.json.JSONObject

/**
 * Событие, на которое вешается действие.
 *
 * SINGLE — обычное одиночное нажатие. По умолчанию не рекомендуется для
 * клавиш навигации: привязка полностью заменяет штатное поведение.
 */
enum class KeyTrigger(val id: String, val label: String) {
    SINGLE("single", "Одиночное"),
    DOUBLE("double", "Двойное"),
    TRIPLE("triple", "Тройное"),
    LONG("long", "Долгое");

    companion object {
        fun fromId(value: String?): KeyTrigger? = entries.firstOrNull { it.id == value }
    }
}

enum class RemapActionType(val id: String, val label: String) {
    GLOBAL("global", "Системное действие"),
    MEDIA("media", "Медиа и громкость"),
    LAUNCH_APP("launch_app", "Запустить приложение"),
    INTENT("intent", "Свой интент");

    companion object {
        fun fromId(value: String?): RemapActionType? = entries.firstOrNull { it.id == value }
    }
}

enum class GlobalAction(val id: String, val label: String) {
    BACK("back", "Назад"),
    HOME("home", "На главный экран"),
    RECENTS("recents", "Недавние приложения"),
    NOTIFICATIONS("notifications", "Панель уведомлений"),
    QUICK_SETTINGS("quick_settings", "Быстрые настройки"),
    SCREENSHOT("screenshot", "Скриншот"),
    NONE("none", "Ничего (просто поглотить)");

    companion object {
        fun fromId(value: String?): GlobalAction? = entries.firstOrNull { it.id == value }
    }
}

enum class MediaAction(val id: String, val label: String) {
    VOLUME_UP("volume_up", "Громкость +"),
    VOLUME_DOWN("volume_down", "Громкость −"),
    PLAY_PAUSE("play_pause", "Воспроизведение / пауза"),
    NEXT("next", "Следующий трек"),
    PREVIOUS("previous", "Предыдущий трек"),
    STOP("stop", "Стоп");

    companion object {
        fun fromId(value: String?): MediaAction? = entries.firstOrNull { it.id == value }
    }
}

data class IntentExtra(val key: String, val type: String, val value: String) {
    companion object {
        val TYPES = listOf("string", "int", "long", "float", "boolean", "string_array", "int_array")
    }
}

data class RemapAction(
    val type: RemapActionType,
    val globalAction: String = GlobalAction.NONE.id,
    val mediaAction: String = MediaAction.PLAY_PAUSE.id,
    val packageName: String = "",
    val intentAction: String = "",
    val intentData: String = "",
    val intentType: String = "",
    val intentComponent: String = "",
    val intentFlags: Int = 0,
    val extras: List<IntentExtra> = emptyList()
) {
    /** Человекочитаемое описание действия для списка и журнала. */
    fun summary(context: Context?): String = when (type) {
        RemapActionType.GLOBAL -> GlobalAction.fromId(globalAction)?.label ?: globalAction
        RemapActionType.MEDIA -> MediaAction.fromId(mediaAction)?.label ?: mediaAction
        RemapActionType.LAUNCH_APP -> {
            val label = if (context != null && packageName.isNotBlank()) {
                appLabel(context, packageName)
            } else null
            "Запуск: ${label ?: packageName.ifBlank { "приложение не выбрано" }}"
        }
        RemapActionType.INTENT -> {
            val target = when {
                intentComponent.isNotBlank() -> intentComponent
                packageName.isNotBlank() -> packageName
                else -> ""
            }
            "Интент: ${intentAction.ifBlank { "без action" }}" + if (target.isNotBlank()) " → $target" else ""
        }
    }

    private fun appLabel(context: Context, pkg: String): String? = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        null
    }
}

data class KeyBinding(
    val keyCode: Int,
    val trigger: KeyTrigger,
    val action: RemapAction,
    val enabled: Boolean = true
)

data class KeyRemapConfig(
    val version: Int = CURRENT_VERSION,
    val enabled: Boolean = true,
    val doublePressTimeoutMs: Int = DEFAULT_DOUBLE_MS,
    val triplePressTimeoutMs: Int = DEFAULT_TRIPLE_MS,
    val longPressTimeoutMs: Int = DEFAULT_LONG_MS,
    val bindings: List<KeyBinding> = emptyList()
) {
    fun bindingsFor(keyCode: Int): List<KeyBinding> =
        bindings.filter { it.enabled && it.keyCode == keyCode }

    /**
     * Приводит конфиг к безопасному виду: чистит дубликаты, ограничивает
     * тайминги и количество привязок. Вызывается при каждом чтении и записи,
     * чтобы повреждённые данные не доходили до службы перехвата.
     */
    fun sanitized(): KeyRemapConfig {
        val seen = HashSet<Pair<Int, KeyTrigger>>()
        val clean = bindings.asSequence()
            .filter { it.keyCode in KeyEvent.KEYCODE_UNKNOWN + 1..KeyEvent.getMaxKeyCode() }
            .filter { it.trigger in KeyTrigger.entries }
            .filter { seen.add(it.keyCode to it.trigger) }
            .take(MAX_BINDINGS)
            .toList()
        return copy(
            enabled = enabled,
            doublePressTimeoutMs = doublePressTimeoutMs.coerceIn(MIN_TIMEOUT_MS, MAX_TIMEOUT_MS),
            triplePressTimeoutMs = triplePressTimeoutMs.coerceIn(MIN_TIMEOUT_MS, MAX_TIMEOUT_MS),
            longPressTimeoutMs = longPressTimeoutMs.coerceIn(MIN_TIMEOUT_MS, MAX_TIMEOUT_MS),
            bindings = clean
        )
    }

    companion object {
        const val CURRENT_VERSION = 1
        const val DEFAULT_DOUBLE_MS = 300
        const val DEFAULT_TRIPLE_MS = 600
        const val DEFAULT_LONG_MS = 500
        const val MIN_TIMEOUT_MS = 150
        const val MAX_TIMEOUT_MS = 1500
        const val MAX_BINDINGS = 64
    }
}

/**
 * Каталог клавиш.
 *
 * [interceptable] — может ли клавиша вообще дойти до службы перехвата.
 * HOME, RECENTS и POWER обрабатываются политикой окна (PhoneWindowManager)
 * до приложения, поэтому приложением не перехватываются ни при каких
 * разрешениях. Это ограничение платформы, и интерфейс обязан его показывать,
 * а не делать вид, что привязка работает.
 */
data class RemapKeyInfo(
    val keyCode: Int,
    val label: String,
    val interceptable: Boolean,
    val note: String = ""
)

object KeyCatalog {
    private val entries = listOf(
        RemapKeyInfo(KeyEvent.KEYCODE_VOLUME_UP, "Громкость +", true),
        RemapKeyInfo(KeyEvent.KEYCODE_VOLUME_DOWN, "Громкость −", true),
        RemapKeyInfo(KeyEvent.KEYCODE_CAMERA, "Кнопка камеры", true),
        RemapKeyInfo(KeyEvent.KEYCODE_HEADSETHOOK, "Кнопка гарнитуры", true),
        RemapKeyInfo(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "Медиа: играть / пауза", true),
        RemapKeyInfo(KeyEvent.KEYCODE_MEDIA_NEXT, "Медиа: следующий", true),
        RemapKeyInfo(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "Медиа: предыдущий", true),
        RemapKeyInfo(KeyEvent.KEYCODE_MEDIA_STOP, "Медиа: стоп", true),
        RemapKeyInfo(KeyEvent.KEYCODE_MEDIA_PLAY, "Медиа: играть", true),
        RemapKeyInfo(KeyEvent.KEYCODE_MEDIA_PAUSE, "Медиа: пауза", true),
        RemapKeyInfo(KeyEvent.KEYCODE_BACK, "Назад", true),
        RemapKeyInfo(KeyEvent.KEYCODE_MENU, "Меню", true),
        RemapKeyInfo(KeyEvent.KEYCODE_SEARCH, "Поиск", true),
        RemapKeyInfo(KeyEvent.KEYCODE_ASSIST, "Ассистент", true),
        RemapKeyInfo(KeyEvent.KEYCODE_VOICE_ASSIST, "Голосовой ассистент", true),
        RemapKeyInfo(KeyEvent.KEYCODE_BRIGHTNESS_DOWN, "Уменьшить яркость", true),
        RemapKeyInfo(KeyEvent.KEYCODE_BRIGHTNESS_UP, "Увеличить яркость", true),
        RemapKeyInfo(KeyEvent.KEYCODE_HOME, "Домой", false, "Перехватывается системой до приложения"),
        RemapKeyInfo(KeyEvent.KEYCODE_APP_SWITCH, "Недавние", false, "Перехватывается системой до приложения"),
        RemapKeyInfo(KeyEvent.KEYCODE_POWER, "Питание", false, "Экран блокировки обрабатывается системой"),
        RemapKeyInfo(KeyEvent.KEYCODE_ENTER, "Enter", true),
        RemapKeyInfo(KeyEvent.KEYCODE_SPACE, "Пробел", true),
        RemapKeyInfo(KeyEvent.KEYCODE_TAB, "Tab", true),
        RemapKeyInfo(KeyEvent.KEYCODE_DEL, "Backspace", true),
        RemapKeyInfo(KeyEvent.KEYCODE_ESCAPE, "Escape", true)
    )

    /** Клавиши, которые имеет смысл предлагать в списке выбора. */
    val selectable: List<RemapKeyInfo> = entries

    private val byCode = entries.associateBy { it.keyCode }

    fun info(keyCode: Int): RemapKeyInfo? = byCode[keyCode]

    fun label(keyCode: Int): String = byCode[keyCode]?.label ?: defaultLabel(keyCode)

    fun isInterceptable(keyCode: Int): Boolean = byCode[keyCode]?.interceptable ?: true

    fun note(keyCode: Int): String = byCode[keyCode]?.note ?: ""

    private fun defaultLabel(keyCode: Int): String {
        val raw = try {
            KeyEvent.keyCodeToString(keyCode)
        } catch (e: Exception) {
            "KEYCODE_$keyCode"
        }
        val trimmed = raw.removePrefix("KEYCODE_").replace('_', ' ').lowercase()
        return trimmed.replaceFirstChar { it.uppercase() }
    }
}

/** Кодирование конфига в строку для Settings.Secure. */
object KeyRemapCodec {

    fun encode(config: KeyRemapConfig): String {
        val root = JSONObject()
        root.put("version", KeyRemapConfig.CURRENT_VERSION)
        root.put("enabled", config.enabled)
        root.put("double_ms", config.doublePressTimeoutMs)
        root.put("triple_ms", config.triplePressTimeoutMs)
        root.put("long_ms", config.longPressTimeoutMs)
        val array = JSONArray()
        config.bindings.forEach { binding ->
            array.put(
                JSONObject().apply {
                    put("key", binding.keyCode)
                    put("trigger", binding.trigger.id)
                    put("enabled", binding.enabled)
                    put("action", encodeAction(binding.action))
                }
            )
        }
        root.put("bindings", array)
        return root.toString()
    }

    /** Возвращает null, если данные повреждены — вызывающий решает, что делать. */
    fun decode(raw: String?): KeyRemapConfig? {
        if (raw.isNullOrBlank()) return KeyRemapConfig()
        return try {
            val root = JSONObject(raw)
            if (root.optInt("version", 0) != KeyRemapConfig.CURRENT_VERSION) return null
            val array = root.optJSONArray("bindings") ?: JSONArray()
            val bindings = ArrayList<KeyBinding>(array.length())
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val keyCode = item.optInt("key", KeyEvent.KEYCODE_UNKNOWN)
                val trigger = KeyTrigger.fromId(item.optString("trigger")) ?: continue
                val action = decodeAction(item.optJSONObject("action")) ?: continue
                bindings.add(
                    KeyBinding(
                        keyCode = keyCode,
                        trigger = trigger,
                        action = action,
                        enabled = item.optBoolean("enabled", true)
                    )
                )
            }
            KeyRemapConfig(
                version = KeyRemapConfig.CURRENT_VERSION,
                enabled = root.optBoolean("enabled", true),
                doublePressTimeoutMs = root.optInt("double_ms", KeyRemapConfig.DEFAULT_DOUBLE_MS),
                triplePressTimeoutMs = root.optInt("triple_ms", KeyRemapConfig.DEFAULT_TRIPLE_MS),
                longPressTimeoutMs = root.optInt("long_ms", KeyRemapConfig.DEFAULT_LONG_MS),
                bindings = bindings
            ).sanitized()
        } catch (e: Exception) {
            null
        }
    }

    private fun encodeAction(action: RemapAction): JSONObject = JSONObject().apply {
        put("type", action.type.id)
        put("global", action.globalAction)
        put("media", action.mediaAction)
        put("package", action.packageName)
        put("action", action.intentAction)
        put("data", action.intentData)
        put("mime", action.intentType)
        put("component", action.intentComponent)
        put("flags", action.intentFlags)
        if (action.extras.isNotEmpty()) {
            val extras = JSONArray()
            action.extras.forEach { extra ->
                extras.put(
                    JSONObject().apply {
                        put("key", extra.key)
                        put("type", extra.type)
                        put("value", extra.value)
                    }
                )
            }
            put("extras", extras)
        }
    }

    private fun decodeAction(json: JSONObject?): RemapAction? {
        if (json == null) return null
        val type = RemapActionType.fromId(json.optString("type")) ?: return null
        val extrasJson = json.optJSONArray("extras")
        val extras = ArrayList<IntentExtra>()
        if (extrasJson != null) {
            for (i in 0 until extrasJson.length()) {
                val item = extrasJson.optJSONObject(i) ?: continue
                val key = item.optString("key")
                if (key.isBlank()) continue
                val extraType = item.optString("type", "string")
                if (extraType !in IntentExtra.TYPES) continue
                extras.add(IntentExtra(key, extraType, item.optString("value")))
            }
        }
        return RemapAction(
            type = type,
            globalAction = json.optString("global", GlobalAction.NONE.id),
            mediaAction = json.optString("media", MediaAction.PLAY_PAUSE.id),
            packageName = json.optString("package"),
            intentAction = json.optString("action"),
            intentData = json.optString("data"),
            intentType = json.optString("mime"),
            intentComponent = json.optString("component"),
            intentFlags = json.optInt("flags", 0),
            extras = extras
        )
    }
}
