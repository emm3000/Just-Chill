package com.emm.justchill.hh.domain.transaction.crud

import com.emm.justchill.hh.domain.account.crud.AccountBalanceUpdater
import com.emm.justchill.hh.domain.shared.UniqueIdProvider
import com.emm.justchill.hh.domain.shared.DateAndTimeCombiner
import com.emm.justchill.hh.domain.transaction.TransactionRepository
import com.emm.justchill.hh.domain.transaction.model.TransactionInsert

class TransactionCreator(
    private val repository: TransactionRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
    private val uniqueIdProvider: UniqueIdProvider,
    private val accountBalanceUpdater: AccountBalanceUpdater,
) {

    suspend fun create(
        amount: String,
        transactionInsert: TransactionInsert,
    ) {

        val transactionId: String = uniqueIdProvider.uniqueId

        val dateAndTimeCombined: Long = dateAndTimeCombiner.combine(transactionInsert.date)

        val amountDouble: Double = amount.toDouble()

        val transaction: TransactionInsert = transactionInsert.copy(
            id = transactionId,
            amount = amountDouble,
            date = dateAndTimeCombined
        )

        repository.create(transaction)

        accountBalanceUpdater.update(accountId = transactionInsert.accountId)
    }
}