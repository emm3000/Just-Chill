package com.emm.domain.recurring

import com.emm.domain.shared.Money

/**
 * Derived monthly totals for all recurring movement templates.
 *
 * Rules (see GetRecurringMonthlyTotalsUseCase):
 * - Only ACTIVE templates (isActive == true) contribute to any field.
 * - Only FIXED templates (amount != null) contribute to incomeTotal / expenseTotal.
 * - Variable templates (amount == null) are counted in activeVariableCount only.
 */
data class RecurringMonthlyTotals(
    /** Sum of amount for active Income templates with non-null amount. */
    val incomeTotal: Money,
    /** Sum of amount for active Spend templates with non-null amount. */
    val expenseTotal: Money,
    /** Count of active templates (Income OR Spend) with amount == null. */
    val activeVariableCount: Int,
) {
    companion object {
        val Empty = RecurringMonthlyTotals(Money.Zero, Money.Zero, 0)
    }
}
