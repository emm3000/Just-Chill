package com.emm.justchill.hh.transaction.data

import com.emm.data.TransactionQueries
import com.emm.domain.transaction.TransactionUpdate
import com.emm.domain.transaction.TransactionUpdateRepository
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
            type = transactionUpdate.type.name,
            amount = transactionUpdate.amount,
            description = transactionUpdate.description,
            date = transactionUpdate.date,
            transactionId = transactionId,
            accountId = transactionUpdate.accountId,
        )
    }
}