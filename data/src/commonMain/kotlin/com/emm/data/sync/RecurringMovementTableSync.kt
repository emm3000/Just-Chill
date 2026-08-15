package com.emm.data.sync

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.emm.data.EmmDatabaseData
import com.emm.data.shared.catchAsDomainException
import com.emm.data.shared.ioDispatcher
import com.emm.data.shared.isSqliteConstraintViolation
import com.emm.data.shared.safeDbCall
import com.emm.domain.shared.logging.DiagnosticsLogger
import com.emm.domain.sync.LocalRevision
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow

private const val TABLE = "recurring_movements"

// `open` for the same single reason TransactionTableSync is: fetchRemotePage is the only seam
// through which a test can drive the REAL pull — this class's own applyRemoteRow, the shared page
// loop and a real SQLite database — with a chosen page of remote rows.
open class RecurringMovementTableSync(
    private val db: EmmDatabaseData,
    client: SupabaseClient,
    logger: DiagnosticsLogger,
) : BaseTableSync<RecurringMovementRowDto>(
    client = client,
    transact = { body -> db.transaction { body() } },
    logger = logger,
    tableName = TABLE,
) {

    // ---------------------------------------------------------------------------
    // Push hooks
    // ---------------------------------------------------------------------------

    override suspend fun selectPendingDtos(userId: String): List<RecurringMovementRowDto> {
        val pending = safeDbCall { db.recurring_movementsQueries.selectPending().executeAsList() }
        // Recurring_movements raw type: isActive is Long (SQLite integer), dayOfMonth is Long.
        // DTO expects Boolean and Int respectively.
        // Defence-in-depth: only push rows owned by the signed-in user. selectPending already
        // filters userId IS NOT NULL, but a row claimed by a different account must never leak.
        return pending
            .filter { it.userId == userId }
            .map { row ->
                RecurringMovementRowDto(
                    id = row.id,
                    name = row.name,
                    type = row.type,
                    amount = row.amount,
                    description = row.description,
                    categoryId = row.categoryId,
                    accountId = row.accountId,
                    frequency = row.frequency,
                    dayOfMonth = row.dayOfMonth.toInt(),
                    isActive = row.isActive != 0L,
                    lastConfirmedPeriod = row.lastConfirmedPeriod,
                    createdAt = row.createdAt,
                    updatedAt = row.updatedAt,
                    userId = row.userId,
                    deletedAt = row.deletedAt,
                )
            }
    }

    override fun pkOf(dto: RecurringMovementRowDto): String = dto.id

    override fun markSynced(pk: String, updatedAt: Long) {
        db.recurring_movementsQueries.markSynced(pk, updatedAt)
    }

    override suspend fun upsertDtos(dtos: List<RecurringMovementRowDto>) {
        // Composite PK (user_id, id) on the remote: the conflict target MUST match so the upsert
        // merges on this user's own row instead of colliding with another tenant's id and failing
        // the RLS check. Default ignoreDuplicates=false keeps resolution=merge-duplicates.
        client.postgrest.from(TABLE).upsert(dtos) { onConflict = "user_id,id" }
    }

    // ---------------------------------------------------------------------------
    // Pull hooks
    // ---------------------------------------------------------------------------

    override val pkColumn: String = "id"

    override suspend fun fetchRemotePage(
        userId: String,
        overlapCursor: String?,
        after: PullPageKey?,
        limit: Int,
    ): List<RecurringMovementRowDto> = client.postgrest.from(TABLE).select {
        filter { applyPageFilter(userId, overlapCursor, after, pkColumn) }
        order("server_updated_at", Order.ASCENDING)
        order(pkColumn, Order.ASCENDING)
        limit(limit.toLong())
    }.decodeList()

    override fun localRevision(pk: String): LocalRevision? = db.recurring_movementsQueries
        .recurringMovementSyncRevision(pk)
        .executeAsOneOrNull()
        ?.let { localRevisionOf(it.updatedAt, it.syncState) }

    /**
     * Two-statement upsert that never triggers an implicit DELETE.
     * INSERT OR IGNORE inserts new rows; UPDATE patches existing ones in-place.
     * `ON DELETE RESTRICT` is therefore never fired, and no table references this one.
     *
     * isActive / dayOfMonth type notes: SQLDelight maps INTEGER columns to Long; the DTO
     * carries Boolean / Int, so we convert before calling the generated queries.
     *
     * Defers the row on an FK constraint failure (the parent account/category has not been
     * pulled/pushed yet — it will retry next cycle once it arrives).
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override fun applyRemoteRow(remote: RecurringMovementRowDto): RemoteRowOutcome = try {
        // SQLDelight generated INTEGER column; Long parameter expected.
        val dayOfMonthLong = remote.dayOfMonth.toLong()
        // SQLDelight generated INTEGER AS Boolean column needs Long (0/1).
        val isActiveLong = if (remote.isActive) 1L else 0L
        val categoryId = categoryIdFor(remote.categoryId, remote.type, remote.id)

        db.recurring_movementsQueries.insertOrIgnoreFromRemote(
            id = remote.id,
            name = remote.name,
            type = remote.type,
            amount = remote.amount,
            description = remote.description,
            categoryId = categoryId,
            accountId = remote.accountId,
            frequency = remote.frequency,
            dayOfMonth = dayOfMonthLong,
            isActive = isActiveLong,
            lastConfirmedPeriod = remote.lastConfirmedPeriod,
            createdAt = remote.createdAt,
            updatedAt = remote.updatedAt,
            userId = remote.userId,
            deletedAt = remote.deletedAt,
        )
        db.recurring_movementsQueries.updateFromRemote(
            name = remote.name,
            type = remote.type,
            amount = remote.amount,
            description = remote.description,
            categoryId = categoryId,
            accountId = remote.accountId,
            frequency = remote.frequency,
            dayOfMonth = dayOfMonthLong,
            isActive = isActiveLong,
            lastConfirmedPeriod = remote.lastConfirmedPeriod,
            createdAt = remote.createdAt,
            updatedAt = remote.updatedAt,
            userId = remote.userId,
            deletedAt = remote.deletedAt,
            id = remote.id,
        )
        RemoteRowOutcome.Applied
    } catch (e: Exception) {
        if (e.isSqliteConstraintViolation()) RemoteRowOutcome.Deferred else throw e
    }

    /**
     * Same three answers as `TransactionTableSync.categoryIdFor`, for the same composite key and
     * the same reasons — absent means defer, mismatched means uncategorized, equal means keep.
     * Written out rather than shared: the two classes have no common place to put it that is not
     * [BaseTableSync], and two of the four tables have no category at all.
     */
    private fun categoryIdFor(categoryId: String?, type: String, pk: String): String? {
        val storedType = categoryId?.let { db.categoriesQueries.typeOf(it).executeAsOneOrNull() }
        if (storedType == null || storedType == type) return categoryId
        logger.warn("pull dropped category of the wrong type table=$TABLE pk=$pk")
        return null
    }

    override fun markPendingForResync(pk: String) {
        db.recurring_movementsQueries.markPendingForResync(pk)
    }

    // Wrapped like every other observe flow in this module: this one feeds the orchestrator's
    // debounced-write trigger, so an unwrapped SQLite throw would escape into an application-scope
    // coroutine instead of arriving as a DomainException.
    override fun pendingCount(): Flow<Long> = db.recurring_movementsQueries.countPending()
        .asFlow()
        .mapToOne(ioDispatcher)
        .catchAsDomainException()
}
