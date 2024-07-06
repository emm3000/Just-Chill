package com.emm.justchill.hh.data.transaction

import com.emm.justchill.Transactions
import com.emm.justchill.hh.data.AllTransactionsRetriever
import com.emm.justchill.hh.domain.TransactionModel
import com.emm.justchill.hh.domain.transaction.TransactionRepository
import kotlinx.coroutines.flow.Flow

class DefaultTransactionRepository(
    private val transactionSaver: TransactionSaver,
    private val transactionRetriever: AllTransactionsRetriever,
    private val transactionCalculator: TransactionCalculator,
    private val transactionSeeder: TransactionSeeder,
) : TransactionRepository {

    override suspend fun add(entity: TransactionInsert) {
        transactionSaver.save(entity)
    }

    override fun all(): Flow<List<Transactions>> {
        return transactionRetriever.retrieve()
    }

    override fun retrieveByDateRange(
        startDateMillis: Long,
        endDateMillis: Long
    ): Flow<List<Transactions>> {
        return transactionRetriever.retrieveByDateRange(startDateMillis, endDateMillis)
    }

    override fun sumIncome(): Flow<Long> {
        return transactionCalculator.sumIncome()
    }

    override fun sumSpend(): Flow<Long> {
        return transactionCalculator.sumSpend()
    }

    override fun difference(): Flow<Long> {
        return transactionCalculator.difference()
    }

    override suspend fun seed(data: List<TransactionModel>) {
        transactionSeeder.seed(data)
    }
}