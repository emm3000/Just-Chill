package com.emm.domain.home

import com.emm.domain.account.AccountRepository
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionWithCategory
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
        flow2 = transactionRepository.fetchAllWithCategory(),
        transform = { _, transactions -> computeFinancialSummary(transactions) },
    )

    private fun computeFinancialSummary(
        transactions: List<TransactionWithCategory>,
    ): HomeData {

        val lastTransactions: List<TransactionWithCategory> = filterTransactionsByCurrentMonth(transactions).take(7)
        val income = lastTransactions.filter { it.type == TransactionType.Income }.sumOf(TransactionWithCategory::amount)
        val spend = lastTransactions.filter { it.type == TransactionType.Spend }.sumOf(TransactionWithCategory::amount)
        val balance = transactions.sumOf { t ->
            if (t.type == TransactionType.Income) t.amount else -t.amount
        }

        return HomeData(
            lastTransactions = lastTransactions,
            income = income,
            spend = spend,
            balance = balance,
        )
    }

    private fun filterTransactionsByCurrentMonth(transactions: List<TransactionWithCategory>): List<TransactionWithCategory> {
        val now: LocalDate = LocalDate.now()

        val firstDayOfMonth: LocalDate = now.withDayOfMonth(1)
        val lastDayOfMonth: LocalDate = now.withDayOfMonth(now.lengthOfMonth())

        return transactions.filter {
            val toLocalDate = Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
            toLocalDate in firstDayOfMonth..lastDayOfMonth
        }
    }
}
