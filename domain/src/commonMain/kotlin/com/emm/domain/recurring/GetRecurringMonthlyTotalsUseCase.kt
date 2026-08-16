package com.emm.domain.recurring

import com.emm.domain.shared.Money
import com.emm.domain.transaction.TransactionType

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
