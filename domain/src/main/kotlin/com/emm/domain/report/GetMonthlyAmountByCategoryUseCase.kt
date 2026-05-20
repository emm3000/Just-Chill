package com.emm.domain.report

import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionType

class GetMonthlyAmountByCategoryUseCase(private val transactionRepository: TransactionRepository) {

    suspend operator fun invoke(yearMonth: YearMonth, type: TransactionType): List<CategoryAmount> {
        val start = yearMonth.startInclusiveMillis()
        val end = yearMonth.endExclusiveMillis()
        return transactionRepository.monthlyAmountByCategory(type, start, end)
    }
}
