package com.emm.justchill.hh.domain.transaction

import com.emm.justchill.hh.data.transaction.TransactionUpdate

interface TransactionUpdateRepository {

    suspend fun update(
        transactionId: String,
        transactionUpdate: TransactionUpdate,
        categoryId: String,
    )
}