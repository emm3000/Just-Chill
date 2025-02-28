package com.emm.domain.transaction

interface TransactionUpdateRepository {

    suspend fun update(
        transactionId: String,
        transactionUpdate: TransactionUpdate,
    )
}