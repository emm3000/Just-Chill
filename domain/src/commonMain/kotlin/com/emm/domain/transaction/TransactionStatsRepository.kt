package com.emm.domain.transaction

import com.emm.domain.report.CategoryAmount
import com.emm.domain.report.MonthCategoryAmounts
import com.emm.domain.report.MonthlySectionStats
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.MonthRange

interface TransactionStatsRepository {

    suspend fun monthlyAmountByCategory(
        type: TransactionType,
        startInclusive: Long,
        endExclusive: Long,
    ): List<CategoryAmount>

    /**
     * Category breakdown for a whole window of months in ONE database round-trip, both types
     * included, returned in the same order as [ranges].
     *
     * The Trends tab needs twelve months of income and expense. Asking month by month and type by
     * type meant ~30 sequential suspend calls on open, each with its own dispatcher hop, and each
     * free to observe a different snapshot of the table if a write landed mid-load.
     */
    suspend fun monthlyAmountByCategoryForRanges(ranges: List<MonthRange>): List<MonthCategoryAmounts>

    suspend fun monthlyStats(type: TransactionType, startInclusive: Long, endExclusive: Long): MonthlySectionStats

    suspend fun topUsedCategoryIds(type: TransactionType, startInclusive: Long, limit: Int): List<CategoryId>

    suspend fun topUsedCombos(type: TransactionType, startInclusive: Long, limit: Int): List<FrequentCombo>

    suspend fun lastUsedAccountId(): AccountId?
}
