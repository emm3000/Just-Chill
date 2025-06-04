package com.emm.justchill.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import com.emm.domain.account.AccountRepository
import com.emm.domain.category.CategoryRepository
import com.emm.domain.transaction.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters), KoinComponent {

    private val accountsSynchronizer: AccountRepository by inject()
    private val categoriesSynchronizer: CategoryRepository by inject()
    private val transactionsSynchronizer: TransactionRepository by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            listOf(
                async { categoriesSynchronizer.sync() },
                async { accountsSynchronizer.sync() },
                async { transactionsSynchronizer.sync() },
            ).awaitAll()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = appContext.syncForegroundInfo()

    companion object {

        fun startUpSyncWork(): OneTimeWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(SyncConstraints)
            .build()
    }
}