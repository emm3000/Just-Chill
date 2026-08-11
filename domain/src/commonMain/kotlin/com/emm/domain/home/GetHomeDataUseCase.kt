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
    private val clock: Clock = Clock.System,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) {

    operator fun invoke(yearMonth: YearMonth = YearMonth.current(clock)): Flow<HomeData> {
        val startOfMonth = yearMonth.startInclusiveDay()
        val startOfNextMonth = yearMonth.endExclusiveDay()
        // Injected next to the clock, not read ambiently: "what is today" is the one date
        // question left that needs a zone, and a zone no test can vary is a zone no test covers.
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

    /**
     * The month figures still fold rows because the screen lists those same rows anyway; the
     * balance does not, because it spans every transaction ever recorded and used to be re-folded
     * from scratch on each emission.
     */
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
