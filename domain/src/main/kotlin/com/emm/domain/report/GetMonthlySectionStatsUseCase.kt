package com.emm.domain.report

import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionType

class GetMonthlySectionStatsUseCase(private val transactionRepository: TransactionRepository) {

    suspend operator fun invoke(yearMonth: YearMonth, type: TransactionType): MonthlySectionStats {
        val start = yearMonth.startInclusiveMillis()
        val end = yearMonth.endExclusiveMillis()
        return transactionRepository.monthlyStats(type, start, end)
    }
}
