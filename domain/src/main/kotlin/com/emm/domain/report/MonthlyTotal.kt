package com.emm.domain.report

import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth

data class MonthlyTotal(val yearMonth: YearMonth, val income: Money, val expense: Money)
