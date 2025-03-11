package com.emm.domain.transaction

import com.emm.domain.account.AccountBalanceUpdater
import com.emm.domain.shared.DateAndTimeCombiner
import com.emm.domain.shared.UniqueIdProvider

class TransactionCreator(
    private val repository: TransactionRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
    private val uniqueIdProvider: UniqueIdProvider,
    private val accountBalanceUpdater: AccountBalanceUpdater,
) {

    suspend fun create(
        transactionInsert: TransactionInsert,
    ) {

        val transactionId: String = uniqueIdProvider.id

        val dateAndTimeCombined: Long = dateAndTimeCombiner.combineWithUtc(transactionInsert.date)

        val transaction: TransactionInsert = transactionInsert.copy(
            id = transactionId,
            date = dateAndTimeCombined
        )

        repository.create(transaction)

        accountBalanceUpdater.update(accountId = transactionInsert.accountId)
    }
}