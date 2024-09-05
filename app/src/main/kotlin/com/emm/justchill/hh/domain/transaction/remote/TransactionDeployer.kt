package com.emm.justchill.hh.domain.transaction.remote

import androidx.work.ListenableWorker
import com.emm.justchill.Transactions
import com.emm.justchill.hh.domain.transaction.TransactionModel
import com.emm.justchill.hh.domain.auth.AuthRepository
import com.emm.justchill.hh.domain.transaction.TransactionRepository
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.firstOrNull

class TransactionDeployer(
    private val supabaseRepository: TransactionSupabaseRepository,
    private val repository: TransactionRepository,
    private val authRepository: AuthRepository,
) {

    suspend fun deploy(transactionId: String): ListenableWorker.Result {

        val transaction: Transactions = repository.find(transactionId).firstOrNull()
            ?: return ListenableWorker.Result.retry()

        val session: UserInfo = authRepository.session()
            ?: return ListenableWorker.Result.retry()

        val transactionModel = TransactionModel(
            transactionId = transactionId,
            type = transaction.type,
            amount = transaction.amount,
            description = transaction.description,
            date = transaction.date,
            userId = session.id
        )

        return try {
            supabaseRepository.upsert(transactionModel)
            ListenableWorker.Result.success()
        } catch (e: Throwable) {
            ListenableWorker.Result.failure()
        }
    }
}