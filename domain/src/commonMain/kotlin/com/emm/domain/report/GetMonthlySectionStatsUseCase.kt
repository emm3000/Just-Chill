package com.emm.domain.report

import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionStatsRepository
import com.emm.domain.transaction.TransactionType

class GetMonthlySectionStatsUseCase(private val transactionStatsRepository: TransactionStatsRepository) {

    suspend operator fun invoke(yearMonth: YearMonth, type: TransactionType): MonthlySectionStats {
        val start = yearMonth.startInclusiveMillis()
        val end = yearMonth.endExclusiveMillis()
        return transactionStatsRepository.monthlyStats(type, start, end)
    }
}
