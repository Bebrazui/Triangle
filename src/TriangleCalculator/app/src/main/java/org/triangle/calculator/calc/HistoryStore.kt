package org.triangle.calculator.calc

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(
    val expression: String,
    val result: String,
    val timestamp: Long
)

/**
 * История вычислений хранится в SharedPreferences в JSON.
 * Битые данные не роняют приложение: история просто очищается.
 *
 * Настройки истории (лимит записей и сохранение между запусками) лежат там же,
 * чтобы не заводить второй источник правды.
 */
class HistoryStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("triangle_calculator", Context.MODE_PRIVATE)

    /** Сколько записей хранить. Всегда в пределах [MIN_ENTRIES]..[MAX_ENTRIES]. */
    var limit: Int
        get() = prefs.getInt(KEY_LIMIT, DEFAULT_LIMIT).coerceIn(MIN_ENTRIES, MAX_ENTRIES)
        set(value) {
            prefs.edit().putInt(KEY_LIMIT, value.coerceIn(MIN_ENTRIES, MAX_ENTRIES)).apply()
        }

    /** Сохранять ли историю между запусками. */
    var persist: Boolean
        get() = prefs.getBoolean(KEY_PERSIST, true)
        set(value) {
            prefs.edit().putBoolean(KEY_PERSIST, value).apply()
        }

    fun load(): List<HistoryEntry> {
        if (!persist) return emptyList()
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val expression = item.optString("expression")
                    val result = item.optString("result")
                    if (expression.isBlank() || result.isBlank()) continue
                    add(
                        HistoryEntry(
                            expression = expression,
                            result = result,
                            timestamp = item.optLong("timestamp", 0L)
                        )
                    )
                    if (size >= limit) break
                }
            }
        } catch (e: Exception) {
            prefs.edit().remove(KEY_HISTORY).apply()
            emptyList()
        }
    }

    fun save(entries: List<HistoryEntry>) {
        try {
            if (!persist) {
                prefs.edit().remove(KEY_HISTORY).apply()
                return
            }
            val array = JSONArray()
            entries.take(limit).forEach { entry ->
                array.put(
                    JSONObject().apply {
                        put("expression", entry.expression)
                        put("result", entry.result)
                        put("timestamp", entry.timestamp)
                    }
                )
            }
            prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
        } catch (e: Exception) {
            // Запись истории не критична для вычислений — не прерываем работу.
        }
    }

    companion object {
        const val MIN_ENTRIES = 10
        const val MAX_ENTRIES = 100
        const val DEFAULT_LIMIT = 50
        val LIMIT_OPTIONS = listOf(10, 25, 50, 100)

        private const val KEY_HISTORY = "history_json"
        private const val KEY_LIMIT = "history_limit"
        private const val KEY_PERSIST = "history_persist"
    }
}
