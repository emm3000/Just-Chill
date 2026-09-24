package com.emm.justchill.core.database.transaction

import com.emm.justchill.core.database.shared.enumValueOrNull
import com.emm.justchill.core.database.shared.safeDbCall
import com.emm.justchill.core.domain.report.CategoryAmount
import com.emm.justchill.core.domain.report.MonthCategoryAmounts
import com.emm.justchill.core.domain.report.MonthlySectionStats
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.MonthRange
import com.emm.justchill.core.domain.transaction.ComboOccurrence
import com.emm.justchill.core.domain.transaction.FrequentCombo
import com.emm.justchill.core.domain.transaction.TransactionStatsRepository
import com.emm.justchill.core.domain.transaction.TransactionType

class DefaultTransactionStatsRepository(private val localDataSource: TransactionStatsLocalDataSource) :
    TransactionStatsRepository {

    override suspend fun monthlyAmountByCategory(
        type: TransactionType,
        startInclusive: String,
        endExclusive: String,
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
        startInclusive: String,
        endExclusive: String,
    ): MonthlySectionStats = safeDbCall {
        val (count, total) = localDataSource.monthlyStats(type, startInclusive, endExclusive)
        val average = if (count == 0L) Money.Zero else Money(total / count)
        MonthlySectionStats(
            movementCount = count.toInt(),
            averageAmount = average,
        )
    }

    override suspend fun topUsedCategoryIds(
        type: TransactionType,
        startInclusive: String,
        limit: Int,
    ): List<CategoryId> = safeDbCall {
        localDataSource.topUsedCategoryIds(type, startInclusive, limit.toLong())
            .map { CategoryId(it) }
    }

    override suspend fun topUsedCombos(
        type: TransactionType,
        startInclusive: String,
        limit: Int,
    ): List<FrequentCombo> = safeDbCall {
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

    override suspend fun comboOccurrences(type: TransactionType, startInclusive: String): List<ComboOccurrence> =
        safeDbCall {
            localDataSource.comboOccurrences(type, startInclusive)
                .mapNotNull { row ->
                    val parsedType = enumValueOrNull<TransactionType>(row.type) ?: return@mapNotNull null
                    ComboOccurrence(
                        accountId = AccountId(row.accountId),
                        categoryId = CategoryId(row.categoryId),
                        type = parsedType,
                        amount = Money(row.amount),
                        occurredAt = row.occurredAt,
                    )
                }
        }

    override suspend fun lastUsedAccountId(): AccountId? = safeDbCall {
        localDataSource.lastUsedAccountId()?.let { AccountId(it) }
    }
}
