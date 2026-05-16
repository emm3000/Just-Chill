package com.emm.justchill.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import com.emm.domain.account.AccountRepository
import com.emm.domain.shared.error.DomainException
import com.emm.domain.transaction.TransactionRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

private const val MAX_RETRY_ATTEMPTS = 5

class SyncWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters), KoinComponent {

    private val accountsSynchronizer: AccountRepository by inject()
    private val transactionsSynchronizer: TransactionRepository by inject()
    private val syncMutex: SyncMutex by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        FirebaseCrashlytics.getInstance().log("SyncWorker attempt #$runAttemptCount")
        try {
            syncMutex.mutex.withLock {
                accountsSynchronizer.sync()
                transactionsSynchronizer.sync()
            }
            Result.success()
        } catch (e: DomainException.Unauthorized) {
            FirebaseCrashlytics.getInstance().recordException(e)
            Result.failure()
        } catch (e: DomainException.NetworkUnavailable) {
            FirebaseCrashlytics.getInstance().recordException(e)
            retryOrFail()
        } catch (e: DomainException.DatabaseError) {
            FirebaseCrashlytics.getInstance().recordException(e)
            retryOrFail()
        } catch (e: DomainException.Unknown) {
            FirebaseCrashlytics.getInstance().recordException(e)
            retryOrFail()
        } catch (e: DomainException) {
            FirebaseCrashlytics.getInstance().recordException(e)
            Result.failure()
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            Result.failure()
        }
    }

    private fun retryOrFail(): Result =
        if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()

    override suspend fun getForegroundInfo(): ForegroundInfo = appContext.syncForegroundInfo()

    companion object {

        fun startUpSyncWork(): OneTimeWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(SyncConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
    }
}