package com.emm.justchill.hh.data.transaction.workers

import androidx.work.ListenableWorker
import com.emm.justchill.hh.data.shared.syncStatus
import com.emm.justchill.hh.data.transaction.TransactionRemoteRepository
import com.emm.justchill.hh.data.transaction.TransactionModel
import com.emm.justchill.hh.domain.auth.AuthRepository
import com.emm.justchill.hh.domain.transaction.SyncStatus
import com.emm.justchill.hh.domain.transaction.TransactionRepository
import com.emm.justchill.hh.domain.transaction.TransactionUpdateRepository
import com.emm.justchill.hh.domain.transaction.model.Transaction
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.firstOrNull

class TransactionDeployer(
    private val supabaseRepository: TransactionRemoteRepository,
    private val repository: TransactionRepository,
    private val updateRepository: TransactionUpdateRepository,
    private val authRepository: AuthRepository,
) {

    suspend fun deploy(transactionId: String): ListenableWorker.Result {

        val transaction: Transaction = repository.findBy(transactionId).firstOrNull()
            ?: return ListenableWorker.Result.failure()

        val session: UserInfo = authRepository.session()
            ?: return ListenableWorker.Result.retry()

        val syncStatus: SyncStatus = syncStatus(transaction.syncStatus)
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
                repository.deleteBy(transactionId)
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
}