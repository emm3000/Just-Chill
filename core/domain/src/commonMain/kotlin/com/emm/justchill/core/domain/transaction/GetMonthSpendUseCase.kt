package com.emm.justchill.core.domain.transaction

import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetMonthSpendUseCase(private val transactionRepository: TransactionRepository) {

    operator fun invoke(yearMonth: YearMonth): Flow<Money> = transactionRepository
        .allInRange(yearMonth.startInclusiveDay(), yearMonth.endExclusiveDay())
        .map(List<Transaction>::spendTotal)
}

private fun List<Transaction>.spendTotal(): Money = Money(
    filter { transaction -> transaction.type == TransactionType.Spend }
        .sumOf { transaction -> transaction.amount.cents },
)
