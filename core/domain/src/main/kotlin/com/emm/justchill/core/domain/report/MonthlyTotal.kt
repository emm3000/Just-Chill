package com.emm.justchill.core.domain.report

import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth

data class MonthlyTotal(val yearMonth: YearMonth, val income: Money, val expense: Money)
