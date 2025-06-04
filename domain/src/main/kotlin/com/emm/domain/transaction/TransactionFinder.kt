package com.emm.domain.transaction

class TransactionFinder(private val repository: TransactionRepository) {

    fun find(transactionId: String): Transaction? {
        return repository.find(transactionId)
    }
}