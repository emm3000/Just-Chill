package com.emm.domain.transaction

import com.emm.domain.account.AccountBalanceUpdater
import com.emm.domain.shared.DateAndTimeCombiner

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