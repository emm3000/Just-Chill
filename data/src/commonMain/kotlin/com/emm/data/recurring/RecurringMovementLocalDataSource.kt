package com.emm.data.recurring

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.EmmDatabaseData
import com.emm.data.Recurring_movementsQueries
import com.emm.domain.recurring.RecurringMovement
import com.emm.domain.recurring.RecurringMovementDetails
import com.emm.domain.recurring.RecurringMovementInsert
import com.emm.domain.shared.currentTimeInMillis
import com.emm.domain.shared.error.DomainException
import com.emm.domain.transaction.TransactionInsert
import com.emm.data.shared.ioDispatcher
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
        .mapToList(ioDispatcher)
        .map { list -> list.asEntity().asExternalModel() }

    fun allActive(): Flow<List<RecurringMovement>> = rmq.selectActive()
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list -> list.asEntity().asExternalModel() }

    fun allWithDetails(): Flow<List<RecurringMovementDetails>> = rmq.selectAllWithDetails()
        .asFlow()
        .mapToList(ioDispatcher)
        .map { list -> list.mapNotNull { it.asExternalModelOrNull() } }

    suspend fun find(id: String): RecurringMovement? = withContext(ioDispatcher) {
        rmq.find(id).executeAsOneOrNull()?.asEntity()?.asExternalModelOrNull()
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun create(insert: RecurringMovementInsert) = withContext(ioDispatcher) {
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

    suspend fun update(id: String, insert: RecurringMovementInsert) = withContext(ioDispatcher) {
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

    suspend fun softDelete(id: String) = withContext(ioDispatcher) {
        val now = currentTimeInMillis()
        rmq.softDelete(deletedAt = now, updatedAt = now, id = id)
        Unit
    }

    suspend fun countLiveByAccount(accountId: String): Long = withContext(ioDispatcher) {
        rmq.countLiveByAccount(accountId).executeAsOne()
    }

    suspend fun nullCategoryOnLiveRows(categoryId: String) = withContext(ioDispatcher) {
        rmq.nullCategoryOnLiveRows(updatedAt = currentTimeInMillis(), categoryId = categoryId)
    }

    /**
     * Atomically:
     * 1. Re-read lastConfirmedPeriod inside the transaction (DB-level idempotency guard)
     * 2. Insert the transaction record
     * 3. Mark the recurring movement confirmed with a caller-supplied [updatedAt]
     *
     * If the template is already confirmed for [period] (TOCTOU race), throws
     * [DomainException.ValidationError] which causes the SQLDelight transaction to roll back,
     * reverting the just-inserted transaction row.
     *
     * [updatedAt] is supplied by the caller (from a single captured `now`) so this method
     * never reads the clock independently.
     */
    suspend fun confirm(insert: TransactionInsert, recurringId: String, period: String) = withContext(ioDispatcher) {
        emmDatabase.transaction {
            // DB-level idempotency guard: re-check inside the transaction to close the TOCTOU window.
            val current = rmq.find(recurringId).executeAsOneOrNull()
            if (current?.lastConfirmedPeriod == period) {
                throw DomainException.ValidationError(
                    "Template '$recurringId' already confirmed for period $period (concurrent write detected)",
                )
            }

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
                updatedAt = insert.updatedAt,
                id = recurringId,
            )
        }
        Unit
    }
}
