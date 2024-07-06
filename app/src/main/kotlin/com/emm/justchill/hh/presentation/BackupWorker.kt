package com.emm.justchill.hh.presentation

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.emm.justchill.R
import com.emm.justchill.hh.domain.BackupManager
import com.emm.justchill.hh.presentation.transaction.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class BackupWorker(
    private val context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters), KoinComponent {

    private val defaultBackupManager: BackupManager by inject(named("supabase"))

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val result: Boolean = defaultBackupManager.backup().single()
        if (result) {
            loggerForTest()
            Result.success()
        } else {
            loggerFailed()
            Result.failure()
        }
    }

    private fun loggerFailed() {
        val message = "El backup se FALLO el ${DateUtils.currentDateAtReadableFormat()}"
        context.getSharedPreferences("random", Context.MODE_PRIVATE)
            .edit()
            .putString(
                "RANDOM",
                message
            )
            .apply()
    }

    private fun loggerForTest() {
        val message = "El backup fue SUCCESS el ${DateUtils.currentDateAtReadableFormat()}"
        context.getSharedPreferences("random", Context.MODE_PRIVATE)
            .edit()
            .putString(
                "RANDOM",
                message
            )
            .apply()
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
}