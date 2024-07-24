package com.emm.justchill.hh.domain.transaction

import com.emm.justchill.hh.data.transaction.TransactionUpdate
import com.emm.justchill.hh.domain.transactioncategory.DateAndTimeCombiner

class TransactionUpdater(
    private val repository: TransactionUpdateRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
) {

    suspend fun update(
        transactionId: String,
        transactionUpdate: TransactionUpdate,
        categoryId: String,
    ) {
        val dateAndTimeCombined: Long = dateAndTimeCombiner.combine(transactionUpdate.date)

        repository.update(
            transactionId = transactionId,
            transactionUpdate = transactionUpdate.copy(
                date = dateAndTimeCombined
            ),
            categoryId = categoryId
        )
    }
}