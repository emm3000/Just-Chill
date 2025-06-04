package com.emm.justchill.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ForegroundInfo
import androidx.work.NetworkType

val SyncConstraints
    get() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

fun Context.syncForegroundInfo() = ForegroundInfo(
    SYNC_NOTIFICATION_ID,
    syncWorkNotification(),
)
private const val SYNC_NOTIFICATION_ID = 0
private const val SYNC_NOTIFICATION_CHANNEL_ID = "SyncNotificationChannel"

private fun Context.syncWorkNotification(): Notification {
    val channel = NotificationChannel(
        SYNC_NOTIFICATION_CHANNEL_ID,
        " k k ",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "getString(R.string.sync_work_notification_channel_description)"
    }
    val notificationManager: NotificationManager? = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    notificationManager?.createNotificationChannel(channel)

    return NotificationCompat.Builder(
        this,
        SYNC_NOTIFICATION_CHANNEL_ID,
    )
        .setSmallIcon(
            android.R.drawable.stat_sys_download,
        )
        .setContentTitle("Estamos sincronizando")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
}