package com.emm.domain.home

import com.emm.domain.account.AccountRepository
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId

class GetHomeDataUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
) {

    operator fun invoke(): Flow<HomeData> {
        val (startOfMonth, startOfNextMonth) = currentMonthRange()
        return combine(
            flow = accountRepository.all(),
            flow2 = transactionRepository.fetchAllWithCategory(),
            flow3 = transactionRepository.fetchAllWithCategoryInRange(startOfMonth, startOfNextMonth),
            transform = { _, allTransactions, currentMonth ->
                computeFinancialSummary(allTransactions, currentMonth)
            },
        )
    }

    private fun computeFinancialSummary(
        allTransactions: List<TransactionWithCategory>,
        currentMonthTransactions: List<TransactionWithCategory>,
    ): HomeData {

        val lastTransactions: List<TransactionWithCategory> = currentMonthTransactions.take(7)
        val income = lastTransactions.filter { it.type == TransactionType.Income }.sumOf(TransactionWithCategory::amount)
        val spend = lastTransactions.filter { it.type == TransactionType.Spend }.sumOf(TransactionWithCategory::amount)
        val balance = allTransactions.sumOf { t ->
            if (t.type == TransactionType.Income) t.amount else -t.amount
        }

        return HomeData(
            lastTransactions = lastTransactions,
            income = income,
            spend = spend,
            balance = balance,
        )
    }

    private fun currentMonthRange(): Pair<Long, Long> {
        val zone: ZoneId = ZoneId.systemDefault()
        val today: LocalDate = LocalDate.now(zone)
        val firstDayOfMonth: LocalDate = today.withDayOfMonth(1)
        val firstDayOfNextMonth: LocalDate = firstDayOfMonth.plusMonths(1)

        val startOfMonth: Long = firstDayOfMonth.atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfNextMonth: Long = firstDayOfNextMonth.atStartOfDay(zone).toInstant().toEpochMilli()
        return startOfMonth to startOfNextMonth
    }
}
