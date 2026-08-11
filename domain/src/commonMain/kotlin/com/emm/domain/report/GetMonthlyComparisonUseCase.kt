package com.emm.domain.report

import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionStatsRepository
import com.emm.domain.transaction.TransactionType

class GetMonthlyComparisonUseCase(private val transactionStatsRepository: TransactionStatsRepository) {

    /**
     * Returns [MonthlyComparison] with the delta % between [yearMonth] and its previous month,
     * or null when there is nothing to compare (previous total is zero).
     */
    suspend operator fun invoke(yearMonth: YearMonth, type: TransactionType): MonthlyComparison? {
        val currentTotal = totalFor(yearMonth, type)
        val previousMonth = yearMonth.previous()
        val previousTotal = totalFor(previousMonth, type)

        if (previousTotal.cents == 0L) return null

        val delta = (((currentTotal.cents - previousTotal.cents).toDouble() / previousTotal.cents) * 100)
            .toInt()

        return MonthlyComparison(
            currentTotal = currentTotal,
            previousTotal = previousTotal,
            deltaPercent = delta,
            absoluteDelta = currentTotal - previousTotal,
        )
    }

    private suspend fun totalFor(yearMonth: YearMonth, type: TransactionType): Money {
        val start = yearMonth.startInclusiveDay()
        val end = yearMonth.endExclusiveDay()
        val items = transactionStatsRepository.monthlyAmountByCategory(type, start, end)
        return items.fold(Money.Zero) { acc, item -> acc + item.amount }
    }
}
