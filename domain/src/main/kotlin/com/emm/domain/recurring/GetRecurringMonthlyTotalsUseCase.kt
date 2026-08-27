package com.emm.domain.recurring

import com.emm.domain.shared.Money
import com.emm.domain.transaction.TransactionType

class GetRecurringMonthlyTotalsUseCase {

    operator fun invoke(list: List<RecurringMovementDetails>): RecurringMonthlyTotals {
        val activeList = list.filter { it.isActive }
        if (activeList.isEmpty()) return RecurringMonthlyTotals.Empty

        val incomeTotal = activeList.fold(Money.Zero) { acc, item ->
            val amount = item.amount
            if (item.type == TransactionType.Income && amount != null) acc + amount else acc
        }

        val expenseTotal = activeList.fold(Money.Zero) { acc, item ->
            val amount = item.amount
            if (item.type == TransactionType.Spend && amount != null) acc + amount else acc
        }

        val activeVariableCount = activeList.count { it.amount == null }

        return RecurringMonthlyTotals(
            incomeTotal = incomeTotal,
            expenseTotal = expenseTotal,
            activeVariableCount = activeVariableCount,
        )
    }
}
