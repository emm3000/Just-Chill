package com.emm.justchill.hh.domain.account.crud

import com.emm.justchill.hh.domain.account.Account
import com.emm.justchill.hh.domain.account.AccountRepository
import com.emm.justchill.hh.domain.transaction.operations.TransactionDifferenceCalculator
import kotlinx.coroutines.flow.firstOrNull

class AccountBalanceUpdater(
    private val repository: AccountRepository,
    private val transactionDifferenceCalculator: TransactionDifferenceCalculator,
) {

    suspend fun update(accountId: String) {

        val account: Account = repository.findBy(accountId).firstOrNull()
            ?: return

        val difference: Double = transactionDifferenceCalculator
            .calculate(accountId)
            .firstOrNull() ?: return

        val newBalance = account.initialBalance + difference

        repository.updateAmount(accountId, newBalance)
    }
}