package com.emm.justchill.core.domain.report

import com.emm.justchill.core.domain.shared.Money

data class SavingsRate(
    val currentRatePercent: Int,
    val deltaPointsVsPrior: Int?,
    val monthly: List<MonthlyTotal>,
    val averageIncome: Money,
    val averageExpense: Money,
    val monthsWithData: Int,
)
