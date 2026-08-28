package com.emm.domain.recurring

import com.emm.domain.shared.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class RecurringMonthlySummary(val activeCount: Int, val monthlyOutflow: Money)

class GetRecurringMonthlySummaryUseCase(
    private val repository: RecurringMovementRepository,
    private val getTotals: GetRecurringMonthlyTotalsUseCase,
) {

    operator fun invoke(): Flow<RecurringMonthlySummary> = repository.allWithDetails().map { templates ->
        RecurringMonthlySummary(
            activeCount = templates.count { it.isActive },
            monthlyOutflow = getTotals(templates).expenseTotal,
        )
    }
}
