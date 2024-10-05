package com.emm.justchill.hh.transaction.data

import com.emm.justchill.TransactionQueries
import com.emm.justchill.Transactions
import com.emm.justchill.hh.auth.domain.AuthRepository
import com.emm.justchill.hh.transaction.domain.SyncStatus
import com.emm.justchill.hh.transaction.domain.TransactionBackupRepository

class DefaultTransactionBackupRepository(
    private val transactionsQueries: TransactionQueries,
    private val networkDataSource: TransactionRemoteRepository,
    private val authRepository: AuthRepository,
): TransactionBackupRepository {

    override suspend fun seed() {
        val transactions: List<TransactionModel> = networkDataSource.retrieve()
        transactionsQueries.transaction {
            populate(transactions)
        }
    }

    override suspend fun backup() {
        val authId: String = authRepository.session()?.id ?: return
        networkDataSource.deleteAll()
        val transactions: List<TransactionModel> = transactionsQueries
            .retrieveAll("")
            .executeAsList()
            .map(Transactions::toModel)
            .map {
                it.copy(
                    userId = authId,
                )
            }

        networkDataSource.upsert(transactions)
    }

    private fun populate(data: List<TransactionModel>) {
        data.forEach {
            transactionsQueries.addTransaction(
                transactionId = it.transactionId,
                type = it.type,
                amount = it.amount,
                description = it.description,
                date = it.date,
                syncStatus = SyncStatus.SYNCED.name,
                categoryId = null,
                accountId = it.accountId
            )
        }
    }
}