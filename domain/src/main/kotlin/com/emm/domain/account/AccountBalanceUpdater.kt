package com.emm.domain.account

import com.emm.domain.transaction.TransactionType

class AccountBalanceUpdater(private val accountUpdateRepository: AccountUpdateRepository) {

    suspend fun update(account: Account, transactionType: TransactionType, amount: Double) {

        val newBalance: Double = when (transactionType) {
            TransactionType.Income -> account.balance + amount
            TransactionType.Spend -> account.balance - amount
        }

        accountUpdateRepository.updateAmount(account.accountId, newBalance)
    }
}