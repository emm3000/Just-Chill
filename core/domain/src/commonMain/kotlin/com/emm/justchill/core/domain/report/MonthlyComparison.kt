package com.emm.justchill.core.domain.report

import com.emm.justchill.core.domain.shared.Money

data class MonthlyComparison(
    val currentTotal: Money,
    val previousTotal: Money,
    val deltaPercent: Int,
    val absoluteDelta: Money,
)
