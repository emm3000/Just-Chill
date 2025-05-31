package com.emm.domain.account

import com.emm.domain.transaction.TransactionType

class AccountBalanceUpdater(
    private val repository: AccountRepository,
    private val accountUpdateRepository: AccountUpdateRepository,
) {

    suspend fun update(accountId: String, transactionType: TransactionType, amount: Double) {

        val account: Account = repository.find(accountId)
            ?: return

        val newBalance: Double = when (transactionType) {
            TransactionType.Income -> account.balance + amount
            TransactionType.Spend -> account.balance - amount
        }

        accountUpdateRepository.updateAmount(accountId, newBalance)
    }
}