package com.emm.domain.transaction

import com.emm.domain.report.CategoryAmount
import com.emm.domain.report.MonthlySectionStats
import com.emm.domain.shared.CategoryId

interface TransactionStatsRepository {

    suspend fun monthlyAmountByCategory(
        type: TransactionType,
        startInclusive: Long,
        endExclusive: Long,
    ): List<CategoryAmount>

    suspend fun monthlyStats(type: TransactionType, startInclusive: Long, endExclusive: Long): MonthlySectionStats

    suspend fun topUsedCategoryIds(type: TransactionType, startInclusive: Long, limit: Int): List<CategoryId>
}
