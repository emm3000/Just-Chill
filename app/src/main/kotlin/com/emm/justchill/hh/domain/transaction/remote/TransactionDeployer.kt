package com.emm.justchill.hh.domain.transaction.remote

import androidx.work.ListenableWorker
import com.emm.justchill.hh.data.transaction.TransactionSupabaseRepository
import com.emm.justchill.hh.data.transaction.TransactionModel
import com.emm.justchill.hh.domain.auth.AuthRepository
import com.emm.justchill.hh.domain.transaction.SyncStatus
import com.emm.justchill.hh.domain.transaction.TransactionRepository
import com.emm.justchill.hh.domain.transaction.TransactionUpdateRepository
import com.emm.justchill.hh.domain.transaction.model.Transaction
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.firstOrNull

class TransactionDeployer(
    private val supabaseRepository: TransactionSupabaseRepository,
    private val repository: TransactionRepository,
    private val updateRepository: TransactionUpdateRepository,
    private val authRepository: AuthRepository,
) {

    suspend fun deploy(transactionId: String): ListenableWorker.Result {

        val transaction: Transaction = repository.find(transactionId).firstOrNull()
            ?: return ListenableWorker.Result.failure()

        val session: UserInfo = authRepository.session()
            ?: return ListenableWorker.Result.retry()

        val syncStatus: SyncStatus = syncStatus(transaction)
            ?: return ListenableWorker.Result.failure()

        when (syncStatus) {

            SyncStatus.PENDING_INSERT -> {
                upsert(transactionId, transaction, session)
                updateRepository.updateStatus(transactionId, SyncStatus.SYNCED)
            }

            SyncStatus.PENDING_UPDATE -> {
                upsert(transactionId, transaction, session)
                updateRepository.updateStatus(transactionId, SyncStatus.SYNCED)
            }

            SyncStatus.PENDING_DELETE -> {
                supabaseRepository.deleteBy(transactionId)
                repository.delete(transactionId)
            }

            SyncStatus.SYNCED -> {
                return ListenableWorker.Result.success()
            }
        }

        return ListenableWorker.Result.success()
    }

    private suspend fun upsert(
        transactionId: String,
        transaction: Transaction,
        session: UserInfo,
    ) {

        val transactionModel = TransactionModel(
            transactionId = transactionId,
            type = transaction.type,
            amount = transaction.amount,
            description = transaction.description,
            date = transaction.date,
            userId = session.id
        )
        supabaseRepository.upsert(transactionModel)
    }

    private fun syncStatus(transaction: Transaction): SyncStatus? {
        return try {
            SyncStatus.valueOf(transaction.syncStatus)
        } catch (e: Throwable) {
            FirebaseCrashlytics.getInstance().recordException(e)
            null
        }
    }
}