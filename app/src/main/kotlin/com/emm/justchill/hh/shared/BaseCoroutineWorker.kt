package com.emm.justchill.hh.shared

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.emm.justchill.R

abstract class BaseCoroutineWorker(
    context: Context,
    parameters: WorkerParameters
): CoroutineWorker(context, parameters) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(1, createNotification())
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(applicationContext, "backup_channel")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("Backup in Progress")
            .setContentText("Your data is being backed up")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }
}