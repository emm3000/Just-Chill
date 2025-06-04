package com.emm.domain.transaction

import com.emm.domain.account.Account
import com.emm.domain.account.AccountUpdateRepository

class TransactionDeleter(
    private val updateRepository: TransactionRepository,
    private val accountUpdateRepository: AccountUpdateRepository,
) {

    suspend fun delete(transaction: Transaction, account: Account) {
        updateRepository.delete(transaction.transactionId)

        val revertBalance: Double = revertBalance(transaction, account.balance)

        accountUpdateRepository.updateAmount(transaction.accountId, revertBalance)
    }

    private fun revertBalance(
        oldTransaction: Transaction,
        oldBalance: Double,
    ): Double = when (TransactionType.valueOf(oldTransaction.type)) {
        TransactionType.Income -> oldBalance - oldTransaction.amount
        TransactionType.Spend -> oldBalance + oldTransaction.amount
    }
}