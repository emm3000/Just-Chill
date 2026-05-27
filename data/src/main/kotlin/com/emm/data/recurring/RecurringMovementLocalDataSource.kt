package com.emm.data.recurring

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.EmmDatabaseData
import com.emm.data.Recurring_movementsQueries
import com.emm.domain.recurring.RecurringMovement
import com.emm.domain.recurring.RecurringMovementInsert
import com.emm.domain.shared.currentTimeInMillis
import com.emm.domain.transaction.TransactionInsert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class RecurringMovementLocalDataSource(private val emmDatabase: EmmDatabaseData) {

    private val rmq: Recurring_movementsQueries
        get() = emmDatabase.recurring_movementsQueries

    fun all(): Flow<List<RecurringMovement>> = rmq.selectAll()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { list -> list.asEntity().asExternalModel() }

    fun allActive(): Flow<List<RecurringMovement>> = rmq.selectActive()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { list -> list.asEntity().asExternalModel() }

    suspend fun find(id: String): RecurringMovement? = withContext(Dispatchers.IO) {
        rmq.find(id).executeAsOneOrNull()?.asEntity()?.asExternalModel()
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun create(insert: RecurringMovementInsert) = withContext(Dispatchers.IO) {
        val entity = insert.toPersistParams(id = Uuid.random().toString())
        rmq.insert(
            id = entity.id,
            name = entity.name,
            type = entity.type,
            amount = entity.amount,
            description = entity.description,
            categoryId = entity.categoryId,
            accountId = entity.accountId,
            frequency = entity.frequency,
            dayOfMonth = entity.dayOfMonth,
            isActive = entity.isActive,
            lastConfirmedPeriod = entity.lastConfirmedPeriod,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
        Unit
    }

    suspend fun update(id: String, insert: RecurringMovementInsert) = withContext(Dispatchers.IO) {
        rmq.update(
            name = insert.name,
            type = insert.type.name,
            amount = insert.amount?.cents,
            description = insert.description,
            categoryId = insert.categoryId?.value,
            accountId = insert.accountId.value,
            dayOfMonth = insert.dayOfMonth.toLong(),
            isActive = if (insert.isActive) 1L else 0L,
            updatedAt = currentTimeInMillis(),
            id = id,
        )
        Unit
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        rmq.delete(id)
        Unit
    }

    /**
     * Atomically:
     * 1. Insert the transaction record
     * 2. Mark the recurring movement as confirmed for the period
     *
     * Both writes share one SQLDelight database transaction — Option A true atomicity.
     */
    suspend fun confirm(insert: TransactionInsert, recurringId: String, period: String) = withContext(Dispatchers.IO) {
        emmDatabase.transaction {
            emmDatabase.transactionsQueries.insert(
                transactionId = insert.id.value,
                type = insert.type.name,
                amount = insert.amount.cents,
                description = insert.description,
                date = insert.date,
                categoryId = insert.categoryId?.value,
                accountId = insert.accountId.value,
                createdAt = insert.createdAt,
                updatedAt = insert.updatedAt,
            )
            rmq.markConfirmed(
                lastConfirmedPeriod = period,
                updatedAt = currentTimeInMillis(),
                id = recurringId,
            )
        }
        Unit
    }
}
