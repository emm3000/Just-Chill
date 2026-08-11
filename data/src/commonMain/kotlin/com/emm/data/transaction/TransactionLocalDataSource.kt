package com.emm.data.transaction

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.emm.data.CompleteTransactions
import com.emm.data.CompleteTransactionsByDateRange
import com.emm.data.SearchTransactions
import com.emm.data.TransactionsQueries
import com.emm.data.shared.ioDispatcher
import com.emm.data.shared.nowMillis
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * Upper bound on search results. Search is global (cross-month) by design, so this is the only
 * read path without a date window; the cap keeps its worst case at a fixed size.
 */
private const val SEARCH_RESULT_CAP = 200L

// One entity, one data source: the function count mirrors the transaction table's operation
// surface, and splitting it would scatter the queries without removing any.
@Suppress("TooManyFunctions")
class TransactionLocalDataSource(private val tq: TransactionsQueries, private val clock: Clock) {

    suspend fun create(transactionInsert: TransactionInsert) = withContext(ioDispatcher) {
        tq.insert(
            transactionId = transactionInsert.id.value,
            type = transactionInsert.type.name,
            amount = transactionInsert.amount.cents,
            description = transactionInsert.description,
            date = transactionInsert.date,
            categoryId = transactionInsert.categoryId?.value,
            accountId = transactionInsert.accountId.value,
            updatedAt = transactionInsert.updatedAt,
            createdAt = transactionInsert.createdAt,
        )
        Unit
    }

    fun all(): Flow<List<Transaction>> = tq
        .all()
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list -> list.asEntity().asExternalModel() }

    fun completeTransactions(): Flow<List<TransactionWithCategoryEntity>> = tq.completeTransactions()
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list -> list.map(CompleteTransactions::asEntity) }

    fun completeTransactionsByDateRange(
        startInclusive: Long,
        endExclusive: Long,
    ): Flow<List<TransactionWithCategoryEntity>> = tq.completeTransactionsByDateRange(startInclusive, endExclusive)
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list -> list.map(CompleteTransactionsByDateRange::asEntity) }

    fun liveTotals(): Flow<TransactionTotalsEntity> = tq.liveTotals()
        .asFlow()
        .mapToOne(ioDispatcher)
        .map { row -> TransactionTotalsEntity(balance = row.balance, movementCount = row.movementCount) }

    // The generated categoryId is non-null: SQLDelight narrows the nullable column through the
    // query's `categoryId IS NOT NULL` filter.
    fun countPerCategory(): Flow<List<CategoryUsageCountEntity>> = tq.countPerCategory()
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list ->
            list.map { row -> CategoryUsageCountEntity(categoryId = row.categoryId, usageCount = row.usageCount) }
        }

    fun searchTransactions(query: String, categoryIds: Set<String>): Flow<List<TransactionWithCategoryEntity>> {
        val queryEmpty: Long = if (query.isBlank()) 1L else 0L
        val categoryFilterEmpty: Long = if (categoryIds.isEmpty()) 1L else 0L
        val safeCategoryIds: Collection<String> =
            if (categoryIds.isEmpty()) listOf("") else categoryIds

        return tq.searchTransactions(
            queryEmpty = queryEmpty,
            query = query.trim(),
            categoryFilterEmpty = categoryFilterEmpty,
            categoryIds = safeCategoryIds,
            limit = SEARCH_RESULT_CAP,
        )
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list -> list.map(SearchTransactions::asEntity) }
    }

    suspend fun countByAccount(accountId: String): Long = withContext(ioDispatcher) {
        tq.countByAccount(accountId).executeAsOne()
    }

    suspend fun countLiveByAccount(accountId: String): Long = withContext(ioDispatcher) {
        tq.countLiveByAccount(accountId).executeAsOne()
    }

    suspend fun softDelete(transactionId: String) = withContext(ioDispatcher) {
        val now = clock.nowMillis()
        tq.softDelete(deletedAt = now, updatedAt = now, transactionId = transactionId)
    }

    fun find(transactionId: String): Transaction? =
        tq.find(transactionId).executeAsOneOrNull()?.asEntity()?.asExternalModelOrNull()

    suspend fun update(transactionId: String, transactionUpdate: TransactionUpdate) = withContext(ioDispatcher) {
        tq.update(
            type = transactionUpdate.type.name,
            amount = transactionUpdate.amount.cents,
            description = transactionUpdate.description,
            date = transactionUpdate.date,
            transactionId = transactionId,
            accountId = transactionUpdate.accountId.value,
            categoryId = transactionUpdate.categoryId?.value,
            updatedAt = clock.nowMillis(),
        )
    }
}
