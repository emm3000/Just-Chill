package com.emm.data.transaction

import com.emm.data.shared.enumValueOrNull
import com.emm.data.shared.safeDbCall
import com.emm.domain.report.CategoryAmount
import com.emm.domain.report.MonthCategoryAmounts
import com.emm.domain.report.MonthlySectionStats
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.MonthRange
import com.emm.domain.transaction.FrequentCombo
import com.emm.domain.transaction.TransactionStatsRepository
import com.emm.domain.transaction.TransactionType

class DefaultTransactionStatsRepository(private val localDataSource: TransactionStatsLocalDataSource) :
    TransactionStatsRepository {

    override suspend fun monthlyAmountByCategory(
        type: TransactionType,
        startInclusive: Long,
        endExclusive: Long,
    ): List<CategoryAmount> = safeDbCall {
        localDataSource.monthlyAmountByCategory(type, startInclusive, endExclusive)
            .map { it.toDomain() }
    }

    override suspend fun monthlyAmountByCategoryForRanges(ranges: List<MonthRange>): List<MonthCategoryAmounts> =
        safeDbCall {
            localDataSource.monthlyAmountByCategoryForRanges(ranges)
                .map { rows -> rows.toMonthCategoryAmounts() }
        }

    override suspend fun monthlyStats(
        type: TransactionType,
        startInclusive: Long,
        endExclusive: Long,
    ): MonthlySectionStats = safeDbCall {
        val (count, total) = localDataSource.monthlyStats(type, startInclusive, endExclusive)
        val average = if (count == 0L) Money.Zero else Money(total / count)
        MonthlySectionStats(
            movementCount = count.toInt(),
            averageAmount = average,
        )
    }

    override suspend fun topUsedCategoryIds(type: TransactionType, startInclusive: Long, limit: Int): List<CategoryId> =
        safeDbCall {
            localDataSource.topUsedCategoryIds(type, startInclusive, limit.toLong())
                .map { CategoryId(it) }
        }

    override suspend fun topUsedCombos(type: TransactionType, startInclusive: Long, limit: Int): List<FrequentCombo> =
        safeDbCall {
            localDataSource.topUsedCombos(type, startInclusive, limit.toLong())
                .mapNotNull { row ->
                    val parsedType = enumValueOrNull<TransactionType>(row.type) ?: return@mapNotNull null
                    FrequentCombo(
                        accountId = AccountId(row.accountId),
                        categoryId = CategoryId(row.categoryId),
                        type = parsedType,
                    )
                }
        }

    override suspend fun lastUsedAccountId(): AccountId? = safeDbCall {
        localDataSource.lastUsedAccountId()?.let { AccountId(it) }
    }
}
