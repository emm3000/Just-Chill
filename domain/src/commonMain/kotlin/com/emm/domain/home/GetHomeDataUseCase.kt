package com.emm.domain.home

import com.emm.domain.recurring.GetPendingRecurringMovementsUseCase
import com.emm.domain.recurring.PendingRecurring
import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionTotals
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class GetHomeDataUseCase(
    private val transactionRepository: TransactionRepository,
    private val getPendingRecurringMovements: GetPendingRecurringMovementsUseCase,
    private val clock: Clock,
    private val zone: TimeZone,
) {

    operator fun invoke(yearMonth: YearMonth = YearMonth.current(clock, zone)): Flow<HomeData> {
        val startOfMonth = yearMonth.startInclusiveDay()
        val startOfNextMonth = yearMonth.endExclusiveDay()
        val today: LocalDate = clock.now().toLocalDateTime(zone).date
        return combine(
            flow = transactionRepository.observeTotals(),
            flow2 = transactionRepository.fetchAllWithCategoryInRange(startOfMonth, startOfNextMonth),
            flow3 = getPendingRecurringMovements(today),
            transform = { totals, currentMonth, pending ->
                computeFinancialSummary(totals, currentMonth, pending)
            },
        )
    }

    private fun computeFinancialSummary(
        totals: TransactionTotals,
        currentMonthTransactions: List<TransactionWithCategory>,
        pendingRecurringMovements: List<PendingRecurring>,
    ): HomeData {
        val lastTransactions: List<TransactionWithCategory> = currentMonthTransactions.take(7)
        val income: Money = currentMonthTransactions
            .filter { it.type == TransactionType.Income }
            .fold(Money.Zero) { acc, t -> acc + t.amount }
        val spend: Money = currentMonthTransactions
            .filter { it.type == TransactionType.Spend }
            .fold(Money.Zero) { acc, t -> acc + t.amount }

        return HomeData(
            lastTransactions = lastTransactions,
            income = income,
            spend = spend,
            balance = totals.balance,
            hasAnyTransaction = totals.movementCount > 0L,
            pendingRecurringMovements = pendingRecurringMovements,
        )
    }
}
