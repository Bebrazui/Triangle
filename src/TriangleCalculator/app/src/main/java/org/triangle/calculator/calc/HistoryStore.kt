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
 */
class HistoryStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("triangle_calculator", Context.MODE_PRIVATE)

    fun load(): List<HistoryEntry> {
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
                }
            }
        } catch (e: Exception) {
            prefs.edit().remove(KEY_HISTORY).apply()
            emptyList()
        }
    }

    fun save(entries: List<HistoryEntry>) {
        try {
            val array = JSONArray()
            entries.take(MAX_ENTRIES).forEach { entry ->
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
        const val MAX_ENTRIES = 50
        private const val KEY_HISTORY = "history_json"
    }
}
