package com.emm.domain.home

import com.emm.domain.shared.Money
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

class GetHomeDataUseCase(
    private val transactionRepository: TransactionRepository,
    private val clock: Clock = Clock.System,
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
        val zone: TimeZone = TimeZone.currentSystemDefault()
        val today = clock.todayIn(zone)
        val firstDayOfMonth = LocalDate(today.year, today.month, 1)
        val firstDayOfNextMonth = firstDayOfMonth.plus(DatePeriod(months = 1))

        val startOfMonth: Long = firstDayOfMonth.atStartOfDayIn(zone).toEpochMilliseconds()
        val startOfNextMonth: Long = firstDayOfNextMonth.atStartOfDayIn(zone).toEpochMilliseconds()
        return startOfMonth to startOfNextMonth
    }
}
