package com.emm.data.transaction

import com.emm.data.TransactionsQueries
import com.emm.data.shared.ioDispatcher
import com.emm.domain.shared.MonthRange
import com.emm.domain.transaction.TransactionType
import kotlinx.coroutines.withContext

class TransactionStatsLocalDataSource(private val tq: TransactionsQueries) {

    suspend fun monthlyAmountByCategory(
        type: TransactionType,
        startInclusive: String,
        endExclusive: String,
    ): List<MonthlyAmountByCategoryEntity> = withContext(ioDispatcher) {
        tq.monthlyAmountByCategory(
            type = type.name,
            startInclusive = startInclusive,
            endExclusive = endExclusive,
        ).executeAsList().map { it.asEntity() }
    }

    /**
     * The read transaction is not decoration: without it the months could be read either side of a
     * concurrent write, and the Trends tab would draw a bar chart no single state of the database
     * ever produced.
     */
    suspend fun monthlyAmountByCategoryForRanges(ranges: List<MonthRange>): List<List<MonthlyAmountByTypeEntity>> =
        withContext(ioDispatcher) {
            tq.transactionWithResult {
                ranges.map { range ->
                    tq.monthlyAmountByCategoryAndType(
                        startInclusive = range.startInclusive,
                        endExclusive = range.endExclusive,
                    ).executeAsList().map { it.asEntity() }
                }
            }
        }

    suspend fun monthlyStats(type: TransactionType, startInclusive: String, endExclusive: String): Pair<Long, Long> =
        withContext(ioDispatcher) {
            val row = tq.monthlyStats(
                type = type.name,
                startInclusive = startInclusive,
                endExclusive = endExclusive,
            ).executeAsOne()
            row.movementCount to row.totalAmount
        }

    suspend fun topUsedCategoryIds(type: TransactionType, startInclusive: String, limit: Long): List<String> =
        withContext(ioDispatcher) {
            tq.topUsedCategoryIds(
                type = type.name,
                startInclusive = startInclusive,
                limit = limit,
            ).executeAsList().filterNotNull()
        }

    suspend fun topUsedCombos(
        type: TransactionType,
        startInclusive: String,
        limit: Long,
    ): List<com.emm.data.TopUsedCombos> = withContext(ioDispatcher) {
        tq.topUsedCombos(
            type = type.name,
            startInclusive = startInclusive,
            limit = limit,
        ).executeAsList()
    }

    suspend fun lastUsedAccountId(): String? = withContext(ioDispatcher) {
        tq.lastUsedAccountId().executeAsOneOrNull()
    }
}
