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

private fun List<Transaction>.spendTotal(): Money {
    var cents = 0L
    forEach { transaction ->
        if (transaction.type == TransactionType.Spend) cents += transaction.amount.cents
    }
    return Money(cents)
}
