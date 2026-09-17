package com.emm.justchill.core.domain.report

import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.domain.transaction.TransactionStatsRepository
import com.emm.justchill.core.domain.transaction.TransactionType

class GetMonthlySectionStatsUseCase(private val transactionStatsRepository: TransactionStatsRepository) {

    suspend operator fun invoke(yearMonth: YearMonth, type: TransactionType): MonthlySectionStats {
        val start = yearMonth.startInclusiveDay()
        val end = yearMonth.endExclusiveDay()
        return transactionStatsRepository.monthlyStats(type, start, end)
    }
}
