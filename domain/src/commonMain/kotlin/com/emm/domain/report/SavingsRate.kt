package com.emm.domain.report

import com.emm.domain.shared.Money

data class SavingsRate(
    val currentRatePercent: Int,
    val deltaPointsVsPrior: Int?,
    val monthly: List<MonthlyTotal>,
    val averageIncome: Money,
    val averageExpense: Money,
)
