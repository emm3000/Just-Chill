package com.emm.data.transaction

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.Transactions
import com.emm.data.TransactionsQueries
import com.emm.domain.shared.SyncState
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
            transactionId = transactionInsert.id,
            type = transactionInsert.type.name,
            amount = transactionInsert.amount,
            description = transactionInsert.description,
            date = transactionInsert.date,
            categoryId = transactionInsert.categoryId,
            accountId = transactionInsert.account.accountId,
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
            .map(List<Transactions>::toDomain)
    }

    suspend fun delete(transactionId: String) = withContext(Dispatchers.IO) {
        tq.softDelete(transactionId)
    }

    fun find(transactionId: String): Transaction? {
        val firstOrNull: Transactions? = tq
            .find(transactionId)
            .executeAsOneOrNull()
        return firstOrNull?.toDomain()
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
            accountId = transactionUpdate.account.accountId,
            syncState = SyncState.Pending.name,
            updatedAt = currentTimeInMillis(),
        )
    }

    suspend fun unSynced(): List<Transactions> = withContext(Dispatchers.IO) {
        tq.selectByStatus(SyncState.Pending.name).executeAsList()
    }
}