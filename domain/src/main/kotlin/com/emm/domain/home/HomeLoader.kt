package com.emm.domain.home

import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HomeLoader(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
) {

    fun load(): Flow<HomeData> = combine(
        flow = accountRepository.all(),
        flow2 = transactionRepository.all(),
        transform = ::computeFinancialSummary,
    )

    private fun computeFinancialSummary(
        accounts: List<Account>,
        transactions: List<Transaction>,
    ): HomeData {

        val lastTransactions = filterTransactionsByCurrentMonth(transactions)
        val balance = accounts.sumOf(Account::balance)
        val income = lastTransactions.filter { it.type == TransactionType.Income }.sumOf(Transaction::amount)
        val spend = lastTransactions.filter { it.type == TransactionType.Spend }.sumOf(Transaction::amount)

        return HomeData(
            lastTransactions = lastTransactions,
            income = income,
            spend = spend,
            balance = balance,
        )
    }

    private fun filterTransactionsByCurrentMonth(transactions: List<Transaction>): List<Transaction> {
        val now: LocalDate = LocalDate.now()

        val firstDayOfMonth: LocalDate = now.withDayOfMonth(1)
        val lastDayOfMonth: LocalDate = now.withDayOfMonth(now.lengthOfMonth())

        return transactions.filter {
            val toLocalDate = Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
            toLocalDate in firstDayOfMonth..lastDayOfMonth
        }
    }
}