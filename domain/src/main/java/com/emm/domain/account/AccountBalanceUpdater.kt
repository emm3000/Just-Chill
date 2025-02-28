package com.emm.domain.account

import com.emm.domain.transaction.TransactionDifferenceCalculator
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