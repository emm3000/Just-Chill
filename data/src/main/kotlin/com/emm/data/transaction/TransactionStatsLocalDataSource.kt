package com.emm.data.transaction

import com.emm.data.TransactionsQueries
import com.emm.domain.transaction.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TransactionStatsLocalDataSource(private val tq: TransactionsQueries) {

    suspend fun monthlyAmountByCategory(
        type: TransactionType,
        startInclusive: Long,
        endExclusive: Long,
    ): List<MonthlyAmountByCategoryEntity> = withContext(Dispatchers.IO) {
        tq.monthlyAmountByCategory(
            type = type.name,
            startInclusive = startInclusive,
            endExclusive = endExclusive,
        ).executeAsList().map { it.asEntity() }
    }

    suspend fun monthlyStats(type: TransactionType, startInclusive: Long, endExclusive: Long): Pair<Long, Long> =
        withContext(Dispatchers.IO) {
            val row = tq.monthlyStats(
                type = type.name,
                startInclusive = startInclusive,
                endExclusive = endExclusive,
            ).executeAsOne()
            row.movementCount to row.totalAmount
        }

    suspend fun topUsedCategoryIds(type: TransactionType, startInclusive: Long, limit: Long): List<String> =
        withContext(Dispatchers.IO) {
            tq.topUsedCategoryIds(
                type = type.name,
                startInclusive = startInclusive,
                limit = limit,
            ).executeAsList().filterNotNull()
        }

    suspend fun topUsedCombos(
        type: TransactionType,
        startInclusive: Long,
        limit: Long,
    ): List<com.emm.data.TopUsedCombos> = withContext(Dispatchers.IO) {
        tq.topUsedCombos(
            type = type.name,
            startInclusive = startInclusive,
            limit = limit,
        ).executeAsList()
    }

    suspend fun lastUsedAccountId(): String? = withContext(Dispatchers.IO) {
        tq.lastUsedAccountId().executeAsOneOrNull()
    }
}
