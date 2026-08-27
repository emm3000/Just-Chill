package com.emm.domain.report

import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionStatsRepository
import com.emm.domain.transaction.TransactionType

class GetMonthlyAmountByCategoryUseCase(private val transactionStatsRepository: TransactionStatsRepository) {

    suspend operator fun invoke(yearMonth: YearMonth, type: TransactionType): List<CategoryAmount> {
        val start = yearMonth.startInclusiveDay()
        val end = yearMonth.endExclusiveDay()
        return transactionStatsRepository.monthlyAmountByCategory(type, start, end)
    }
}
