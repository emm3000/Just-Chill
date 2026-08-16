package com.emm.domain.report

import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionStatsRepository
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

private const val PERCENT_MULTIPLIER = 100

class GetSavingsRateUseCase(private val transactionStatsRepository: TransactionStatsRepository) {

    suspend operator fun invoke(clock: Clock, zone: TimeZone, months: Int = 6): SavingsRate {
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

    private fun averageOver(total: Money, monthsWithData: Int): Money =
        if (monthsWithData == 0) Money.Zero else Money(total.cents / monthsWithData)

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

    private fun savingsRate(income: Money, expense: Money): Int {
        if (income.cents == 0L) return 0
        return ((income.cents - expense.cents).toDouble() / income.cents * PERCENT_MULTIPLIER).toInt()
    }
}
