package com.emm.justchill.core.domain.transaction

import com.emm.justchill.core.domain.report.CategoryAmount
import com.emm.justchill.core.domain.report.MonthCategoryAmounts
import com.emm.justchill.core.domain.report.MonthlySectionStats
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.MonthRange

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
