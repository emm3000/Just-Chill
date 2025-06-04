package com.emm.domain.transaction

import com.emm.domain.account.Account
import com.emm.domain.account.AccountUpdateRepository
import com.emm.domain.shared.DateAndTimeCombiner

class TransactionUpdater(
    private val repository: TransactionUpdateRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
    private val accountUpdateRepository: AccountUpdateRepository,
) {

    suspend fun update(
        oldTransaction: Transaction,
        oldAccount: Account,
        transactionUpdate: TransactionUpdate,
    ) {

        val dateAndTimeCombined: Long = dateAndTimeCombiner.combineWithUtc(transactionUpdate.date)

        val updatedTransaction: TransactionUpdate = transactionUpdate.copy(date = dateAndTimeCombined)
        repository.update(
            transactionId = oldTransaction.transactionId,
            transactionUpdate = updatedTransaction,
        )

        if (oldAccount.accountId == transactionUpdate.account.accountId) {
            revertThenUpdateBalance(transactionUpdate, oldTransaction)
            return
        }

        updatedBalanceFromOldAccount(oldAccount, oldTransaction)

        updateBalanceFromNewAccountPicked(transactionUpdate)
    }

    private suspend fun revertThenUpdateBalance(
        transactionUpdate: TransactionUpdate,
        oldTransaction: Transaction,
    ) {
        val currentBalance: Double = transactionUpdate.account.balance

        val revertedBalance: Double = revertBalance(oldTransaction, currentBalance)
        val newBalance: Double = calculateNewBalance(transactionUpdate, revertedBalance)
        accountUpdateRepository.updateAmount(transactionUpdate.account.accountId, newBalance)
    }

    private suspend fun updateBalanceFromNewAccountPicked(transactionUpdate: TransactionUpdate) {
        val newBalance = calculateNewBalance(transactionUpdate, transactionUpdate.account.balance)
        accountUpdateRepository.updateAmount(transactionUpdate.account.accountId, newBalance)
    }

    private suspend fun updatedBalanceFromOldAccount(oldAccount: Account, oldTransaction: Transaction) {
        val currentBalance: Double = oldAccount.balance
        val revertedBalance: Double = revertBalance(oldTransaction, currentBalance)
        accountUpdateRepository.updateAmount(oldAccount.accountId, revertedBalance)
    }

    private fun calculateNewBalance(
        transactionUpdate: TransactionUpdate,
        balance: Double,
    ): Double = when (transactionUpdate.type) {
        TransactionType.Income -> balance + transactionUpdate.amount
        TransactionType.Spend -> balance - transactionUpdate.amount
    }

    private fun revertBalance(
        oldTransaction: Transaction,
        oldBalance: Double,
    ): Double = when (TransactionType.valueOf(oldTransaction.type)) {
        TransactionType.Income -> oldBalance - oldTransaction.amount
        TransactionType.Spend -> oldBalance + oldTransaction.amount
    }
}