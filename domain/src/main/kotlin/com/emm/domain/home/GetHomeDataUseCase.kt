package com.emm.domain.home

import com.emm.domain.shared.Money
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId

class GetHomeDataUseCase(
    private val transactionRepository: TransactionRepository,
) {

    operator fun invoke(): Flow<HomeData> {
        val (startOfMonth, startOfNextMonth) = currentMonthRange()
        return combine(
            flow = transactionRepository.fetchAllWithCategory(),
            flow2 = transactionRepository.fetchAllWithCategoryInRange(startOfMonth, startOfNextMonth),
            transform = { allTransactions, currentMonth ->
                computeFinancialSummary(allTransactions, currentMonth)
            },
        )
    }

    private fun computeFinancialSummary(
        allTransactions: List<TransactionWithCategory>,
        currentMonthTransactions: List<TransactionWithCategory>,
    ): HomeData {

        val lastTransactions: List<TransactionWithCategory> = currentMonthTransactions.take(7)
        val income: Money = lastTransactions
            .filter { it.type == TransactionType.Income }
            .fold(Money.Zero) { acc, t -> acc + t.amount }
        val spend: Money = lastTransactions
            .filter { it.type == TransactionType.Spend }
            .fold(Money.Zero) { acc, t -> acc + t.amount }
        val balance: Money = allTransactions.fold(Money.Zero) { acc, t ->
            if (t.type == TransactionType.Income) acc + t.amount else acc + (-t.amount)
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
