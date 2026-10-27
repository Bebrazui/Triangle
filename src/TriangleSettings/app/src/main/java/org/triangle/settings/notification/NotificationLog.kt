package org.triangle.settings.notification

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Локальный журнал уведомлений.
 *
 * Наполняется [NotificationLogService], когда пользователь включает доступ
 * к уведомлениям. Хранится в SharedPreferences приложения и ограничен
 * [MAX_ENTRIES] последними записями — никаких выдуманных данных: в журнале
 * только то, что реально пришло в систему.
 */
object NotificationLog {

    data class Entry(
        val key: String,
        val packageName: String,
        val appName: String,
        val title: String,
        val text: String,
        val channelId: String,
        val postedAt: Long,
        val isOngoing: Boolean
    )

    const val MAX_ENTRIES = 300
    private const val PREFS = "triangle_notification_log"
    private const val KEY_ENTRIES = "entries"

    private val lock = Any()

    fun entries(context: Context): List<Entry> = synchronized(lock) { read(context) }

    /** Новая запись сверху; повторный ключ (обновление уведомления) заменяет старую. */
    fun add(context: Context, entry: Entry) = synchronized(lock) {
        val current = read(context).toMutableList()
        current.removeAll { it.key == entry.key }
        current.add(0, entry)
        trim(current)
        write(context, current)
    }

    fun clear(context: Context) = synchronized(lock) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ENTRIES)
            .apply()
    }

    /** Каналы, которые реально встречались в записях приложения: id → количество. */
    fun channelsFor(context: Context, packageName: String): List<Pair<String, Int>> =
        entries(context)
            .asSequence()
            .filter { it.packageName == packageName && it.channelId.isNotEmpty() }
            .groupingBy { it.channelId }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }

    fun countFor(context: Context, packageName: String): Int =
        entries(context).count { it.packageName == packageName }

    private fun trim(list: MutableList<Entry>) {
        while (list.size > MAX_ENTRIES) list.removeAt(list.lastIndex)
    }

    private fun read(context: Context): List<Entry> {
        return try {
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_ENTRIES, null) ?: return emptyList()
            val array = JSONArray(raw)
            val result = ArrayList<Entry>(array.length())
            for (i in 0 until array.length()) {
                val json = array.optJSONObject(i) ?: continue
                val key = json.optString("key")
                if (key.isEmpty()) continue
                result.add(
                    Entry(
                        key = key,
                        packageName = json.optString("pkg"),
                        appName = json.optString("app"),
                        title = json.optString("title"),
                        text = json.optString("text"),
                        channelId = json.optString("channel"),
                        postedAt = json.optLong("time"),
                        isOngoing = json.optBoolean("ongoing")
                    )
                )
            }
            result
        } catch (e: Exception) {
            android.util.Log.w("NotificationLog", "cannot read log", e)
            emptyList()
        }
    }

    private fun write(context: Context, list: List<Entry>) {
        try {
            val array = JSONArray()
            for (entry in list) {
                array.put(
                    JSONObject()
                        .put("key", entry.key)
                        .put("pkg", entry.packageName)
                        .put("app", entry.appName)
                        .put("title", entry.title)
                        .put("text", entry.text)
                        .put("channel", entry.channelId)
                        .put("time", entry.postedAt)
                        .put("ongoing", entry.isOngoing)
                )
            }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ENTRIES, array.toString())
                .apply()
        } catch (e: Exception) {
            android.util.Log.w("NotificationLog", "cannot write log", e)
        }
    }
}
