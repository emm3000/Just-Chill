package com.emm.domain.report

import com.emm.domain.shared.Money

/**
 * [monthsWithData] is how many months of [monthly] actually hold movements. It is the divisor
 * behind [averageIncome] / [averageExpense], and it is what tells the UI whether the window is
 * long enough for the trend to mean anything.
 */
data class SavingsRate(
    val currentRatePercent: Int,
    val deltaPointsVsPrior: Int?,
    val monthly: List<MonthlyTotal>,
    val averageIncome: Money,
    val averageExpense: Money,
    val monthsWithData: Int,
)
