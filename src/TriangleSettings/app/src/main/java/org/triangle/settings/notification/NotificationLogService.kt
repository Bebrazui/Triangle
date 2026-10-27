package org.triangle.settings.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Служба доступа к уведомлениям: записывает реальные оповещения в
 * [NotificationLog]. Работает только после того, как пользователь включил
 * доступ к уведомлениям для TriangleSettings.
 */
class NotificationLogService : NotificationListenerService() {

    override fun onListenerConnected() {
        connected = true
        snapshotActive()
    }

    override fun onListenerDisconnected() {
        connected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val sbn = sbn ?: return
        val notification = sbn.notification ?: return
        // Групповые сводки не несут полезного текста и дублируют дочерние записи.
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        val appName = try {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (e: Exception) {
            sbn.packageName
        }

        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = (
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)
            )?.toString()?.trim().orEmpty()

        NotificationLog.add(
            this,
            NotificationLog.Entry(
                key = sbn.key,
                packageName = sbn.packageName,
                appName = appName,
                title = title,
                text = text,
                channelId = notification.channelId.orEmpty(),
                postedAt = sbn.postTime,
                isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
            )
        )
        android.util.Log.d(
            "NotificationLogService",
            "logged ${sbn.packageName} channel=${notification.channelId}"
        )
    }

    /** При подключении забираем уже висящие уведомления, чтобы журнал не пустовал. */
    private fun snapshotActive() {
        try {
            activeNotifications?.forEach { onNotificationPosted(it) }
        } catch (e: Exception) {
            android.util.Log.w("NotificationLogService", "cannot read active notifications", e)
        }
    }

    companion object {
        @Volatile
        var connected: Boolean = false
            private set
    }
}
