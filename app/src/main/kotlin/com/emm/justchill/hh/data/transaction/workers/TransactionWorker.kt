package com.emm.justchill.hh.data.transaction.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.emm.justchill.hh.data.shared.BaseCoroutineWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TransactionWorker(
    context: Context,
    parameters: WorkerParameters,
) : BaseCoroutineWorker(context, parameters), KoinComponent {

    private val transactionDeployer: TransactionDeployer by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val transactionId: String = inputData.getString("transactionId")
            ?: return@withContext Result.failure()
        return@withContext transactionDeployer.deploy(transactionId)
    }

    companion object {

        fun startUpSyncWork(transactionId: String): OneTimeWorkRequest {

            val constraints: Constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data: Data = workDataOf(
                "transactionId" to transactionId
            )

            return OneTimeWorkRequestBuilder<TransactionWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(constraints)
                .setInputData(data)
                .build()
        }
    }
}