package com.emm.justchill.hh.shared

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import com.emm.justchill.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BackupWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters), KoinComponent {

    private val defaultBackupManager: BackupManager by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val result: Boolean = defaultBackupManager.backup()
        if (result) {
            Result.success()
        } else {
            Result.retry()
        }
    }

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

    companion object {

        fun startUpSyncWork(): OneTimeWorkRequest {

            val constraints: Constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return OneTimeWorkRequestBuilder<BackupWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(constraints)
                .build()
        }
    }
}