package com.emm.domain.account

import com.emm.domain.transaction.TransactionType
import kotlinx.coroutines.flow.firstOrNull

class AccountBalanceUpdater(private val repository: AccountRepository) {

    suspend fun update(accountId: String, transactionType: TransactionType, amount: Double) {

        val account: Account = repository.findBy(accountId).firstOrNull()
            ?: return

        val newBalance: Double = when (transactionType) {
            TransactionType.Income -> account.balance + amount
            TransactionType.Spend -> account.balance - amount
        }

        repository.updateAmount(accountId, newBalance)
    }
}