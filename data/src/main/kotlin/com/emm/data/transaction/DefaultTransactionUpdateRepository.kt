package com.emm.data.transaction

import com.emm.data.TransactionsQueries
import com.emm.domain.transaction.TransactionUpdate
import com.emm.domain.transaction.TransactionUpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultTransactionUpdateRepository(
    private val transactionQueries: TransactionsQueries,
) : TransactionUpdateRepository {

    override suspend fun update(
        transactionId: String,
        transactionUpdate: TransactionUpdate,
    ) = withContext(Dispatchers.IO) {
        transactionQueries.update(
            type = transactionUpdate.type.name,
            amount = transactionUpdate.amount,
            description = transactionUpdate.description,
            date = transactionUpdate.date,
            transactionId = transactionId,
            accountId = transactionUpdate.accountId,
        )
    }
}