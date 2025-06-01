package com.emm.data.transaction

import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionRepository
import kotlinx.coroutines.flow.Flow

class DefaultTransactionRepository(
    private val localDataSource: TransactionLocalDataSource,
) : TransactionRepository {

    override suspend fun create(transactionInsert: TransactionInsert) {
        return localDataSource.create(transactionInsert)
    }

    override fun retrieve(): Flow<List<Transaction>> {
        return localDataSource.retrieve()
    }

    override fun sumIncome(accountId: String): Flow<Double> {
        return localDataSource.sumIncome(accountId)
    }

    override fun sumSpend(accountId: String): Flow<Double> {
        return localDataSource.sumSpend(accountId)
    }

    override fun difference(accountId: String): Flow<Double> {
        return localDataSource.difference(accountId)
    }

    override suspend fun delete(transactionId: String) {
        localDataSource.delete(transactionId)
    }

    override fun find(transactionId: String): Flow<Transaction?> {
        return localDataSource.find(transactionId)
    }
}