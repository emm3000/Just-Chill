package com.emm.justchill.hh.data.workers

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.emm.justchill.R
import com.emm.justchill.hh.domain.transaction.remote.TransactionDeployer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TransactionAddedWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters), KoinComponent {

    private val transactionDeployer: TransactionDeployer by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val transactionId: String = inputData.getString("transactionId")
            ?: return@withContext Result.retry()
        return@withContext transactionDeployer.deploy(transactionId)
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

        fun startUpSyncWork(transactionId: String): OneTimeWorkRequest {

            val constraints: Constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data: Data = workDataOf(
                "transactionId" to transactionId
            )

            return OneTimeWorkRequestBuilder<TransactionAddedWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(constraints)
                .setInputData(data)
                .build()
        }
    }
}