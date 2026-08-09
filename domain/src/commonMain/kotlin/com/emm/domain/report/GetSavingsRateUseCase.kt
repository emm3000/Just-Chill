package com.emm.domain.report

import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionStatsRepository
import com.emm.domain.transaction.TransactionType
import kotlin.time.Clock

private const val PERCENT_MULTIPLIER = 100

class GetSavingsRateUseCase(private val transactionStatsRepository: TransactionStatsRepository) {

    suspend operator fun invoke(months: Int = 6, clock: Clock = Clock.System): SavingsRate {
        val current = YearMonth.current(clock)
        val currentWindow = buildWindow(current, months)
        val priorWindow = buildWindow(currentWindow.first().yearMonth.previous(), months)

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

        val averageIncome = Money(totalIncomeCurrent.cents / months)
        val averageExpense = Money(totalExpenseCurrent.cents / months)

        return SavingsRate(
            currentRatePercent = currentRatePercent,
            deltaPointsVsPrior = deltaPoints,
            monthly = currentWindow,
            averageIncome = averageIncome,
            averageExpense = averageExpense,
        )
    }

    private suspend fun buildWindow(endMonthInclusive: YearMonth, months: Int): List<MonthlyTotal> {
        // Build oldest-first list of `months` months ending at endMonthInclusive
        val result = mutableListOf<MonthlyTotal>()
        var ym = endMonthInclusive
        repeat(months) {
            val start = ym.startInclusiveMillis()
            val end = ym.endExclusiveMillis()
            val incomeItems = transactionStatsRepository.monthlyAmountByCategory(TransactionType.Income, start, end)
            val expenseItems = transactionStatsRepository.monthlyAmountByCategory(TransactionType.Spend, start, end)
            val income = incomeItems.fold(Money.Zero) { acc, item -> acc + item.amount }
            val expense = expenseItems.fold(Money.Zero) { acc, item -> acc + item.amount }
            result.add(0, MonthlyTotal(ym, income, expense))
            ym = ym.previous()
        }
        return result
    }

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
