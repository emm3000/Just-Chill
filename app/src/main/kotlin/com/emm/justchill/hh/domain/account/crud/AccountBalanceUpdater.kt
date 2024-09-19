package com.emm.justchill.hh.domain.account.crud

import com.emm.justchill.hh.domain.account.Account
import com.emm.justchill.hh.domain.account.AccountRepository
import com.emm.justchill.hh.presentation.transaction.TransactionType
import kotlinx.coroutines.flow.firstOrNull

class AccountBalanceUpdater(private val repository: AccountRepository) {

    suspend fun update(
        accountId: String,
        balance: Double,
        transactionType: TransactionType,
    ) {

        val account: Account = repository.findBy(accountId).firstOrNull()
            ?: return

        val newBalance = when (transactionType) {
            TransactionType.INCOME -> account.balance + balance
            TransactionType.SPENT -> account.balance - balance
        }

        repository.updateAmount(accountId, newBalance)
    }
}