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
        startInclusive: String,
        endExclusive: String,
    ): List<CategoryAmount>

    // One entry per range, in the same order as [ranges]: callers pair the two lists by index.
    suspend fun monthlyAmountByCategoryForRanges(ranges: List<MonthRange>): List<MonthCategoryAmounts>

    suspend fun monthlyStats(type: TransactionType, startInclusive: String, endExclusive: String): MonthlySectionStats

    suspend fun topUsedCategoryIds(type: TransactionType, startInclusive: String, limit: Int): List<CategoryId>

    suspend fun topUsedCombos(type: TransactionType, startInclusive: String, limit: Int): List<FrequentCombo>

    suspend fun lastUsedAccountId(): AccountId?
}
