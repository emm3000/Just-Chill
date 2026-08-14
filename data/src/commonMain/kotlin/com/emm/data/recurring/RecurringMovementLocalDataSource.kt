package com.emm.data.recurring

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.EmmDatabaseData
import com.emm.data.Recurring_movementsQueries
import com.emm.data.shared.ioDispatcher
import com.emm.data.shared.nowMillis
import com.emm.data.shared.toOccurredAtText
import com.emm.domain.recurring.RecurringMovement
import com.emm.domain.recurring.RecurringMovementDetails
import com.emm.domain.recurring.RecurringMovementInsert
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import com.emm.domain.transaction.TransactionInsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class RecurringMovementLocalDataSource(private val emmDatabase: EmmDatabaseData, private val clock: Clock) {

    private val rmq: Recurring_movementsQueries
        get() = emmDatabase.recurring_movementsQueries

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
        val entity = insert.toPersistParams(id = Uuid.random().toString(), now = clock.nowMillis())
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
            updatedAt = clock.nowMillis(),
            id = id,
        )
        Unit
    }

    suspend fun softDelete(id: String) = withContext(ioDispatcher) {
        val now = clock.nowMillis()
        rmq.softDelete(deletedAt = now, updatedAt = now, id = id)
        Unit
    }

    suspend fun countLiveByAccount(accountId: String): Long = withContext(ioDispatcher) {
        rmq.countLiveByAccount(accountId).executeAsOne()
    }

    /**
     * Atomically:
     * 1. Re-read lastConfirmedPeriod inside the transaction (DB-level idempotency guard)
     * 2. Insert the transaction record
     * 3. Mark the recurring movement confirmed with the same timestamp
     *
     * If the template is already settled at or past [period] (TOCTOU race), throws
     * [DomainException.ValidationError] which causes the SQLDelight transaction to roll back,
     * reverting the just-inserted transaction row.
     *
     * The clock is read ONCE, before the transaction opens, and the value stamps the new row and
     * the template's high-water mark alike. Two rows written atomically that disagree about when
     * would be a lie about an operation that either happened or did not.
     */
    suspend fun confirm(insert: TransactionInsert, recurringId: String, period: String) = withContext(ioDispatcher) {
        val now = clock.nowMillis()
        emmDatabase.transaction {
            // DB-level idempotency guard: re-check inside the transaction to close the TOCTOU window.
            ensureNotSettled(recurringId, period)

            emmDatabase.transactionsQueries.insert(
                transactionId = insert.id.value,
                type = insert.type.name,
                amount = insert.amount.cents,
                description = insert.description,
                occurredAt = insert.occurredAt.toOccurredAtText(),
                categoryId = insert.categoryId?.value,
                accountId = insert.accountId.value,
                createdAt = now,
                updatedAt = now,
            )
            rmq.markConfirmed(
                lastConfirmedPeriod = period,
                updatedAt = now,
                id = recurringId,
            )
        }
        Unit
    }

    /**
     * Advances the high-water mark to [period] with no transaction attached.
     *
     * Same guard as [confirm], for the same reason: a concurrent write must not be able to rewind
     * the mark and resurrect periods the user already settled.
     */
    suspend fun skip(recurringId: String, period: String, updatedAt: Long) = withContext(ioDispatcher) {
        emmDatabase.transaction {
            ensureNotSettled(recurringId, period)
            rmq.markConfirmed(lastConfirmedPeriod = period, updatedAt = updatedAt, id = recurringId)
        }
        Unit
    }

    /**
     * Throws unless [period] is strictly newer than the stored mark.
     *
     * Period keys are zero-padded "YYYY-MM", so string ordering is chronological ordering and the
     * comparison needs no parsing.
     *
     * **Paired guard:** `parsePeriodKey` in `:domain` decides the same question — "is this mark
     * settled" — and is the only one of the two that bounds what a well-formed key is. They must
     * agree. A key that parser rejects still reaches this comparison as a raw string and sorts
     * wherever its bytes fall, so a mark it calls malformed can settle every future period here
     * while `pendingPeriods` goes on listing them as owed: confirm and skip both throw, and the
     * template is stuck. Change that bound and re-read this.
     */
    private fun ensureNotSettled(recurringId: String, period: String) {
        val settledThrough = rmq.find(recurringId).executeAsOneOrNull()?.lastConfirmedPeriod ?: return
        if (settledThrough >= period) {
            throw DomainException.ValidationError(
                "Template '$recurringId' is already settled through $settledThrough (concurrent write detected)",
                ValidationCode.RecurringAlreadyConfirmed,
            )
        }
    }
}
