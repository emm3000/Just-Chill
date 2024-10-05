package com.emm.justchill.hh.category.data.worker

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

class CategoryWorker(
    context: Context,
    parameters: WorkerParameters,
): BaseCoroutineWorker(context, parameters), KoinComponent {

    private val categoryDeployer: CategoryDeployer by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val categoryId: String = inputData.getString("categoryId")
            ?: return@withContext Result.failure()
        return@withContext categoryDeployer.deploy(categoryId)
    }

    companion object {

        fun startUpSyncWork(categoryId: String): OneTimeWorkRequest {

            val constraints: Constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data: Data = workDataOf(
                "categoryId" to categoryId
            )

            return OneTimeWorkRequestBuilder<CategoryWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(constraints)
                .setInputData(data)
                .build()
        }
    }
}