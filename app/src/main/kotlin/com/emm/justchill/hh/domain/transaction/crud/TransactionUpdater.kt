package com.emm.justchill.hh.domain.transaction.crud

import com.emm.justchill.hh.domain.shared.DateAndTimeCombiner
import com.emm.justchill.hh.domain.transaction.TransactionUpdateRepository
import com.emm.justchill.hh.domain.transaction.model.TransactionUpdate

class TransactionUpdater(
    private val repository: TransactionUpdateRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
) {

    suspend fun update(
        transactionId: String,
        transactionUpdate: TransactionUpdate,
    ) {

        val dateAndTimeCombined: Long = dateAndTimeCombiner.combine(transactionUpdate.date)

        repository.update(
            transactionId = transactionId,
            transactionUpdate = transactionUpdate.copy(
                date = dateAndTimeCombined
            ),
        )
    }
}