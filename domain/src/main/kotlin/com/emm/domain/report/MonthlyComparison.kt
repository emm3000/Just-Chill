package com.emm.domain.report

import com.emm.domain.shared.Money

data class MonthlyComparison(
    val currentTotal: Money,
    val previousTotal: Money,
    val deltaPercent: Int,
)
