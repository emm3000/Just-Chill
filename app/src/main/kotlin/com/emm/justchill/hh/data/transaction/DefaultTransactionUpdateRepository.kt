package com.emm.justchill.hh.data.transaction

import com.emm.justchill.TransactionQueries
import com.emm.justchill.hh.domain.transaction.TransactionUpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultTransactionUpdateRepository(
    private val transactionQueries: TransactionQueries,
) : TransactionUpdateRepository {

    override suspend fun update(
        transactionId: String,
        transactionUpdate: TransactionUpdate,
    ) = withContext(Dispatchers.IO) {
        transactionQueries.updateValues(
            type = transactionUpdate.type,
            amount = transactionUpdate.amount,
            description = transactionUpdate.description,
            date = transactionUpdate.date,
            transactionId = transactionId
        )
    }
}