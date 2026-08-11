package com.emm.domain.report

import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionStatsRepository
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

private const val PERCENT_MULTIPLIER = 100

class GetSavingsRateUseCase(private val transactionStatsRepository: TransactionStatsRepository) {

    /**
     * [clock] and [zone] have no defaults on purpose. "Which month is it" is a question about a
     * place, and a default that reads the machine lets a caller answer it for the wrong one without
     * saying so — which is exactly how Reporte ended up resolving its months against the ambient
     * zone while every other screen took an injected one. Callers state where they are asking from.
     */
    suspend operator fun invoke(clock: Clock, zone: TimeZone, months: Int = 6): SavingsRate {
        // Both windows are contiguous and end at the current month, so they are one fetch:
        // the prior window is the older half of a 2 * months span.
        val span = buildSpan(YearMonth.current(clock, zone), months * 2)
        val priorWindow = span.take(months)
        val currentWindow = span.drop(months)

        val totalIncomeCurrent = currentWindow.fold(Money.Zero) { acc, m -> acc + m.income }
        val totalExpenseCurrent = currentWindow.fold(Money.Zero) { acc, m -> acc + m.expense }
        val totalIncomePrior = priorWindow.fold(Money.Zero) { acc, m -> acc + m.income }

        val currentRatePercent = savingsRate(totalIncomeCurrent, totalExpenseCurrent)
        val priorRatePercent = savingsRate(
            totalIncomePrior,
            priorWindow.fold(Money.Zero) { acc, m -> acc + m.expense },
        )

        // Null delta when prior period has no income at all (no useful baseline)
        val deltaPoints: Int? = if (totalIncomePrior.cents == 0L) {
            null
        } else {
            currentRatePercent - priorRatePercent
        }

        val monthsWithData = currentWindow.count { it.income.cents > 0 || it.expense.cents > 0 }

        return SavingsRate(
            currentRatePercent = currentRatePercent,
            deltaPointsVsPrior = deltaPoints,
            monthly = currentWindow,
            averageIncome = averageOver(totalIncomeCurrent, monthsWithData),
            averageExpense = averageOver(totalExpenseCurrent, monthsWithData),
            monthsWithData = monthsWithData,
        )
    }

    /**
     * Monthly average over the months that actually hold movements, not over the window.
     *
     * Dividing by the fixed window length reported a sixth of reality to every user whose history
     * is shorter than it — which is every user for their first five months.
     *
     * Income and expense deliberately share one divisor. Giving each its own ("months with
     * income", "months with expense") would make the two numbers describe different time spans,
     * and they are read side by side.
     */
    private fun averageOver(total: Money, monthsWithData: Int): Money =
        if (monthsWithData == 0) Money.Zero else Money(total.cents / monthsWithData)

    /** Oldest-first totals for the [months] months ending at [endMonthInclusive], in one round-trip. */
    private suspend fun buildSpan(endMonthInclusive: YearMonth, months: Int): List<MonthlyTotal> {
        val yearMonths = YearMonth.windowEndingAt(endMonthInclusive, months)
        val slices = transactionStatsRepository.monthlyAmountByCategoryForRanges(yearMonths.map { it.range() })
        return yearMonths.mapIndexed { index, ym ->
            val slice = slices.getOrElse(index) { MonthCategoryAmounts.Empty }
            MonthlyTotal(
                yearMonth = ym,
                income = slice.income.total(),
                expense = slice.expense.total(),
            )
        }
    }

    private fun List<CategoryAmount>.total(): Money = fold(Money.Zero) { acc, item -> acc + item.amount }

    /**
     * Share of income left after expenses, in percentage points.
     *
     * Deliberately NOT floored at zero. Spending more than you earn is exactly what a savings
     * rate exists to surface, and clamping it to 0% hid that — an overspending user saw the same
     * number as one who broke even, and the delta against the prior window, built from two
     * clamped values, reported "same pace" while the gap was closing or widening.
     *
     * There is no upper clamp either: with a non-negative expense the result cannot exceed 100.
     */
    private fun savingsRate(income: Money, expense: Money): Int {
        if (income.cents == 0L) return 0
        return ((income.cents - expense.cents).toDouble() / income.cents * PERCENT_MULTIPLIER).toInt()
    }
}
