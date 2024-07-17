package com.emm.justchill.hh.domain.transaction

import com.emm.justchill.hh.data.transaction.TransactionUpdate

class TransactionUpdater(private val repository: TransactionRepository) {

    suspend fun update(transactionId: String, transactionUpdate: TransactionUpdate) {
        repository.update(transactionId = transactionId, transactionUpdate = transactionUpdate)
    }
}