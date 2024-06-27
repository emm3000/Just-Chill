package com.emm.justchill.experiences.hh.data.transaction

import com.emm.justchill.Transactions
import com.emm.justchill.experiences.hh.data.AllItemsRetriever
import com.emm.justchill.experiences.hh.domain.transaction.TransactionRepository
import kotlinx.coroutines.flow.Flow

class DefaultTransactionRepository(
    private val transactionSaver: TransactionSaver,
    private val transactionRetriever: AllItemsRetriever<Transactions>,
) : TransactionRepository {

    override suspend fun add(entity: TransactionInsert) {
        transactionSaver.save(entity)
    }

    override fun all(): Flow<List<Transactions>> {
        return transactionRetriever.retrieve()
    }
}