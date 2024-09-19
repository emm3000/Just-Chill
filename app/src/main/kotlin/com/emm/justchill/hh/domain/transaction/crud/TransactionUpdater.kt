package com.emm.justchill.hh.domain.transaction.crud

import com.emm.justchill.hh.domain.account.crud.AccountBalanceUpdater
import com.emm.justchill.hh.domain.shared.DateAndTimeCombiner
import com.emm.justchill.hh.domain.transaction.TransactionUpdateRepository
import com.emm.justchill.hh.domain.transaction.model.TransactionUpdate
import com.emm.justchill.hh.presentation.transaction.TransactionType

class TransactionUpdater(
    private val repository: TransactionUpdateRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
    private val accountBalanceUpdater: AccountBalanceUpdater,
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
        accountBalanceUpdater.update(
            accountId = transactionUpdate.accountId,
            balance = transactionUpdate.amount,
            transactionType = transactionUpdate.type
        )
    }
}