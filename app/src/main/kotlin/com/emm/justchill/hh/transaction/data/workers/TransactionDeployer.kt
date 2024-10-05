package com.emm.justchill.hh.transaction.data.workers

import androidx.work.ListenableWorker
import com.emm.justchill.hh.shared.syncStatus
import com.emm.justchill.hh.transaction.data.TransactionRemoteRepository
import com.emm.justchill.hh.transaction.data.TransactionModel
import com.emm.justchill.hh.account.domain.AccountBalanceUpdater
import com.emm.justchill.hh.auth.domain.AuthRepository
import com.emm.justchill.hh.transaction.domain.SyncStatus
import com.emm.justchill.hh.transaction.domain.TransactionRepository
import com.emm.justchill.hh.transaction.domain.TransactionUpdateRepository
import com.emm.justchill.hh.transaction.domain.Transaction
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.firstOrNull

class TransactionDeployer(
    private val supabaseRepository: TransactionRemoteRepository,
    private val repository: TransactionRepository,
    private val updateRepository: TransactionUpdateRepository,
    private val authRepository: AuthRepository,
    private val transactionBalanceUpdater: AccountBalanceUpdater,
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
                transactionBalanceUpdater.update(transaction.accountId)
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
            userId = session.id,
            accountId = transaction.accountId,
            categoryId = transaction.categoryId,
        )
        supabaseRepository.upsert(transactionModel)
    }
}