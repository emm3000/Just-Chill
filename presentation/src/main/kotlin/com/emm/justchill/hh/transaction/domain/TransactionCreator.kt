package com.emm.justchill.hh.transaction.domain

import com.emm.justchill.hh.account.domain.AccountBalanceUpdater
import com.emm.justchill.hh.shared.UniqueIdProvider
import com.emm.justchill.hh.shared.DateAndTimeCombiner

class TransactionCreator(
    private val repository: TransactionRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
    private val uniqueIdProvider: UniqueIdProvider,
    private val accountBalanceUpdater: AccountBalanceUpdater,
) {

    suspend fun create(
        transactionInsert: TransactionInsert,
    ) {

        val transactionId: String = uniqueIdProvider.uniqueId

        val dateAndTimeCombined: Long = dateAndTimeCombiner.combineWithUtc(transactionInsert.date)

        val transaction: TransactionInsert = transactionInsert.copy(
            id = transactionId,
            date = dateAndTimeCombined
        )

        repository.create(transaction)

        accountBalanceUpdater.update(accountId = transactionInsert.accountId)
    }
}