package com.emm.domain.home

import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class HomeLoader(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
) {

    fun load(): Flow<HomeData> = combine(
        flow = accountRepository.all(),
        flow2 = transactionRepository.all(),
        transform = ::computeFinancialSummary
    )

    private fun computeFinancialSummary(
        accounts: List<Account>,
        transactions: List<Transaction>,
    ): HomeData {

        val balance = accounts.sumOf { it.balance }
        val income = transactions.filter { it.type == TransactionType.Income }.sumOf { it.amount }
        val spend = transactions.filter { it.type == TransactionType.Spend }.sumOf { it.amount }
        return HomeData(
            lastTransactions = transactions.take(5),
            income = income,
            spend = spend,
            balance = balance,
        )
    }
}