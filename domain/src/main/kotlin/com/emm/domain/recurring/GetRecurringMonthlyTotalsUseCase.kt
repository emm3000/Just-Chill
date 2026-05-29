package com.emm.domain.recurring

import com.emm.domain.shared.Money
import com.emm.domain.transaction.TransactionType

/**
 * Pure calculator: derives [RecurringMonthlyTotals] from an already-loaded detail list.
 *
 * No repository subscription — the ViewModel passes the same list it uses to partition
 * active/paused rows, satisfying Decision 2 (single source of truth, no second DB query).
 *
 * Aggregation rules:
 * - Only ACTIVE templates (isActive == true) contribute to any field.
 * - incomeTotal  = sum of amount.cents for active Income templates with amount != null.
 * - expenseTotal = sum of amount.cents for active Spend templates with amount != null.
 * - activeVariableCount = count of active templates where amount == null.
 * - Paused templates (isActive == false) are excluded from ALL three aggregations.
 * - Variable templates (amount == null) are excluded from incomeTotal and expenseTotal.
 */
class GetRecurringMonthlyTotalsUseCase {

    operator fun invoke(list: List<RecurringMovementDetails>): RecurringMonthlyTotals {
        val activeList = list.filter { it.isActive }
        if (activeList.isEmpty()) return RecurringMonthlyTotals.Empty

        val incomeTotal = activeList
            .filter { it.type == TransactionType.Income && it.amount != null }
            .fold(Money.Zero) { acc, item -> acc + item.amount!! }

        val expenseTotal = activeList
            .filter { it.type == TransactionType.Spend && it.amount != null }
            .fold(Money.Zero) { acc, item -> acc + item.amount!! }

        val activeVariableCount = activeList.count { it.amount == null }

        return RecurringMonthlyTotals(
            incomeTotal = incomeTotal,
            expenseTotal = expenseTotal,
            activeVariableCount = activeVariableCount,
        )
    }
}
