package com.emm.data.transaction

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.TransactionsQueries
import com.emm.domain.shared.SyncState
import com.emm.domain.shared.currentTimeInMillis
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionUpdate
import com.emm.domain.transaction.TransactionWithCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TransactionLocalDataSource(private val tq: TransactionsQueries) {

    suspend fun create(transactionInsert: TransactionInsert) = withContext(Dispatchers.IO) {
        tq.insert(
            transactionId = transactionInsert.id,
            type = transactionInsert.type.name,
            amount = transactionInsert.amount,
            description = transactionInsert.description,
            date = transactionInsert.date,
            categoryId = transactionInsert.categoryId,
            accountId = transactionInsert.accountId,
            syncState = SyncState.Pending.name,
            isDeleted = false,
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

    fun completeTransactions(): Flow<List<TransactionWithCategory>> {
        return tq.completeTransactions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map(::toDomain)
    }

    suspend fun softDelete(transactionId: String) = withContext(Dispatchers.IO) {
        tq.softDelete(currentTimeInMillis(), transactionId)
    }

    suspend fun hardDelete(transactionId: String) = withContext(Dispatchers.IO) {
        tq.delete(transactionId)
    }

    fun find(transactionId: String): Transaction? {
        return tq.find(transactionId).executeAsOneOrNull()?.asEntity()?.asExternalModel()
    }

    fun findEntity(transactionId: String): TransactionEntity? {
        return tq.find(transactionId).executeAsOneOrNull()?.asEntity()
    }

    suspend fun insertSynced(entity: TransactionEntity) = withContext(Dispatchers.IO) {
        tq.insert(
            transactionId = entity.transactionId,
            type = entity.type,
            amount = entity.amount,
            description = entity.description,
            date = entity.date,
            categoryId = entity.categoryId,
            accountId = entity.accountId,
            syncState = SyncState.Synced.name,
            isDeleted = entity.isDeleted,
            updatedAt = entity.updatedAt,
            createdAt = entity.createdAt,
        )
        Unit
    }

    suspend fun update(
        transactionId: String,
        transactionUpdate: TransactionUpdate,
    ) = withContext(Dispatchers.IO) {
        tq.update(
            type = transactionUpdate.type.name,
            amount = transactionUpdate.amount,
            description = transactionUpdate.description,
            date = transactionUpdate.date,
            transactionId = transactionId,
            accountId = transactionUpdate.accountId,
            categoryId = transactionUpdate.categoryId,
            syncState = SyncState.Pending.name,
            updatedAt = currentTimeInMillis(),
        )
    }

    suspend fun markAsSynced(transactionId: String) = withContext(Dispatchers.IO) {
        tq.markAsSync(SyncState.Synced.name, transactionId)
    }

    suspend fun unSynced(): List<TransactionEntity> = withContext(Dispatchers.IO) {
        tq.selectPendingSync().executeAsList().asEntity()
    }
}
