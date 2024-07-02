package com.emm.justchill.experiences.hh.data.transaction

import com.emm.justchill.Transactions
import com.emm.justchill.experiences.hh.data.AllTransactionsRetriever
import com.emm.justchill.experiences.hh.domain.transaction.TransactionRepository
import kotlinx.coroutines.flow.Flow

class DefaultTransactionRepository(
    private val transactionSaver: TransactionSaver,
    private val transactionRetriever: AllTransactionsRetriever,
    private val transactionCalculator: TransactionCalculator,
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
}