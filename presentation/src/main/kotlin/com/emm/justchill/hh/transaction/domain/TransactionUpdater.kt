package com.emm.justchill.hh.transaction.domain

import com.emm.justchill.hh.account.domain.AccountBalanceUpdater
import com.emm.justchill.hh.shared.DateAndTimeCombiner

class TransactionUpdater(
    private val repository: TransactionUpdateRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
    private val accountBalanceUpdater: AccountBalanceUpdater,
) {

    suspend fun update(
        transactionId: String,
        transactionUpdate: TransactionUpdate,
    ) {

        val dateAndTimeCombined: Long = dateAndTimeCombiner.combineWithUtc(transactionUpdate.date)

        repository.update(
            transactionId = transactionId,
            transactionUpdate = transactionUpdate.copy(
                date = dateAndTimeCombined
            ),
        )
        accountBalanceUpdater.update(accountId = transactionUpdate.accountId)
    }
}