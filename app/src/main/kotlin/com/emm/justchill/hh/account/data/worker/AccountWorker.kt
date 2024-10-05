package com.emm.justchill.hh.account.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.emm.justchill.hh.shared.BaseCoroutineWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AccountWorker(
    context: Context,
    parameters: WorkerParameters,
): BaseCoroutineWorker(context, parameters), KoinComponent {

    private val accountDeployer: AccountDeployer by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val accountId: String = inputData.getString("accountId")
            ?: return@withContext Result.failure()
        return@withContext accountDeployer.deploy(accountId)
    }

    companion object {

        fun startUpSyncWork(accountId: String): OneTimeWorkRequest {

            val constraints: Constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data: Data = workDataOf(
                "accountId" to accountId
            )

            return OneTimeWorkRequestBuilder<AccountWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(constraints)
                .setInputData(data)
                .build()
        }
    }
}