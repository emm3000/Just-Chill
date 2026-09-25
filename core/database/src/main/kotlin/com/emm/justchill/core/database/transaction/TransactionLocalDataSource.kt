package com.emm.justchill.core.database.transaction

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.emm.justchill.core.database.CompleteTransactions
import com.emm.justchill.core.database.CompleteTransactionsByDateRange
import com.emm.justchill.core.database.SearchTransactions
import com.emm.justchill.core.database.TransactionsQueries
import com.emm.justchill.core.database.shared.ioDispatcher
import com.emm.justchill.core.database.shared.nowMillis
import com.emm.justchill.core.database.shared.toOccurredAtText
import com.emm.justchill.core.domain.transaction.Transaction
import com.emm.justchill.core.domain.transaction.TransactionInsert
import com.emm.justchill.core.domain.transaction.TransactionUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock

// Search is global (cross-month) by design, so this is the only read path without a date window;
// the cap keeps its worst case at a fixed size.
private const val SEARCH_RESULT_CAP = 200L

class TransactionLocalDataSource(private val tq: TransactionsQueries, private val clock: Clock) {

    suspend fun create(transactionInsert: TransactionInsert) = withContext(ioDispatcher) {
        val now = clock.nowMillis()
        tq.insert(
            transactionId = transactionInsert.id.value,
            type = transactionInsert.type.name,
            amount = transactionInsert.amount.cents,
            description = transactionInsert.description,
            occurredAt = transactionInsert.occurredAt.toOccurredAtText(),
            categoryId = transactionInsert.categoryId?.value,
            accountId = transactionInsert.accountId.value,
            updatedAt = now,
            createdAt = now,
        )
        Unit
    }

    fun completeTransactions(): Flow<List<TransactionWithCategoryEntity>> = tq.completeTransactions()
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list -> list.map(CompleteTransactions::asEntity) }

    fun completeTransactionsByDateRange(
        startInclusive: String,
        endExclusive: String,
    ): Flow<List<TransactionWithCategoryEntity>> = tq.completeTransactionsByDateRange(startInclusive, endExclusive)
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list -> list.map(CompleteTransactionsByDateRange::asEntity) }

    fun byDateRange(startInclusive: String, endExclusive: String): Flow<List<Transaction>> = tq
        .transactionsByDateRange(startInclusive, endExclusive)
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list -> list.asEntity().asExternalModel() }

    fun liveTotals(): Flow<TransactionTotalsEntity> = tq.liveTotals()
        .asFlow()
        .mapToOne(ioDispatcher)
        .map { row -> TransactionTotalsEntity(balance = row.balance, movementCount = row.movementCount) }

    fun countPerCategory(): Flow<List<CategoryUsageCountEntity>> = tq.countPerCategory()
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list ->
            list.map { row -> CategoryUsageCountEntity(categoryId = row.categoryId, usageCount = row.usageCount) }
        }

    fun searchTransactions(
        query: String,
        categoryIds: Set<String>,
        minAmountCents: Long? = null,
        maxAmountCents: Long? = null,
    ): Flow<List<TransactionWithCategoryEntity>> {
        val queryEmpty: Long = if (query.isBlank()) 1L else 0L
        val categoryFilterEmpty: Long = if (categoryIds.isEmpty()) 1L else 0L
        val safeCategoryIds: Collection<String> =
            if (categoryIds.isEmpty()) listOf("") else categoryIds

        return tq.searchTransactions(
            queryEmpty = queryEmpty,
            query = query.trim(),
            categoryFilterEmpty = categoryFilterEmpty,
            categoryIds = safeCategoryIds,
            minUnbounded = if (minAmountCents == null) 1L else 0L,
            minAmount = minAmountCents ?: 0L,
            maxUnbounded = if (maxAmountCents == null) 1L else 0L,
            maxAmount = maxAmountCents ?: 0L,
            limit = SEARCH_RESULT_CAP,
        )
            .asFlow()
            .mapToList(ioDispatcher)
            .map { list -> list.map(SearchTransactions::asEntity) }
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
            occurredAt = transactionUpdate.occurredAt.toOccurredAtText(),
            transactionId = transactionId,
            accountId = transactionUpdate.accountId.value,
            categoryId = transactionUpdate.categoryId?.value,
            updatedAt = clock.nowMillis(),
        )
    }
}
