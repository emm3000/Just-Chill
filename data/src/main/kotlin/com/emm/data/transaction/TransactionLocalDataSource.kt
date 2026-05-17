package com.emm.data.transaction

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.CompleteTransactions
import com.emm.data.CompleteTransactionsByDateRange
import com.emm.data.SearchTransactions
import com.emm.data.TransactionsQueries
import com.emm.domain.shared.currentTimeInMillis
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TransactionLocalDataSource(private val tq: TransactionsQueries) {

    suspend fun create(transactionInsert: TransactionInsert) = withContext(Dispatchers.IO) {
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

    fun all(): Flow<List<Transaction>> {
        return tq
            .all()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.asEntity().asExternalModel() }
    }

    fun completeTransactions(): Flow<List<TransactionWithCategoryEntity>> {
        return tq.completeTransactions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map(CompleteTransactions::asEntity) }
    }

    fun completeTransactionsByDateRange(
        startInclusive: Long,
        endExclusive: Long,
    ): Flow<List<TransactionWithCategoryEntity>> {
        return tq.completeTransactionsByDateRange(startInclusive, endExclusive)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map(CompleteTransactionsByDateRange::asEntity) }
    }

    fun searchTransactions(
        query: String,
        categoryIds: Set<String>,
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
        )
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map(SearchTransactions::asEntity) }
    }

    suspend fun countByAccount(accountId: String): Long = withContext(Dispatchers.IO) {
        tq.countByAccount(accountId).executeAsOne()
    }

    suspend fun delete(transactionId: String) = withContext(Dispatchers.IO) {
        tq.delete(transactionId)
    }

    fun find(transactionId: String): Transaction? {
        return tq.find(transactionId).executeAsOneOrNull()?.asEntity()?.asExternalModel()
    }

    suspend fun update(
        transactionId: String,
        transactionUpdate: TransactionUpdate,
    ) = withContext(Dispatchers.IO) {
        tq.update(
            type = transactionUpdate.type.name,
            amount = transactionUpdate.amount.cents,
            description = transactionUpdate.description,
            date = transactionUpdate.date,
            transactionId = transactionId,
            accountId = transactionUpdate.accountId.value,
            categoryId = transactionUpdate.categoryId?.value,
            updatedAt = currentTimeInMillis(),
        )
    }
}
