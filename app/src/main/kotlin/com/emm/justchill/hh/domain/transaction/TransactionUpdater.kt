package com.emm.justchill.hh.domain.transaction

import com.emm.justchill.hh.data.transaction.TransactionUpdate

class TransactionUpdater(private val repository: TransactionUpdateRepository) {

    suspend fun update(
        transactionId: String,
        transactionUpdate: TransactionUpdate,
        categoryId: String,
    ) {
        repository.update(
            transactionId = transactionId,
            transactionUpdate = transactionUpdate,
            categoryId = categoryId
        )
    }
}