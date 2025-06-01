package com.emm.data.transaction

import com.emm.domain.transaction.TransactionUpdate
import com.emm.domain.transaction.TransactionUpdateRepository

class DefaultTransactionUpdateRepository(private val localDataSource: TransactionLocalDataSource) : TransactionUpdateRepository {

    override suspend fun update(transactionId: String, transactionUpdate: TransactionUpdate) {
        localDataSource.update(transactionId, transactionUpdate)
    }
}