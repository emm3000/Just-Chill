package com.emm.domain.recurring

import com.emm.domain.shared.Money

data class RecurringMonthlyTotals(val incomeTotal: Money, val expenseTotal: Money, val activeVariableCount: Int) {
    companion object {
        val Empty = RecurringMonthlyTotals(Money.Zero, Money.Zero, 0)
    }
}
