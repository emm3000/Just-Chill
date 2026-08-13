package com.emm.data.sync

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.emm.data.EmmDatabaseData
import com.emm.data.shared.catchAsDomainException
import com.emm.data.shared.ioDispatcher
import com.emm.data.shared.isSqliteConstraintViolation
import com.emm.data.shared.localDateTimeFromFixedPeru
import com.emm.data.shared.safeDbCall
import com.emm.data.shared.toFixedPeruEpochMillis
import com.emm.data.shared.toOccurredAtOrNull
import com.emm.data.shared.toOccurredAtText
import com.emm.domain.sync.LocalRevision
import com.emm.domain.sync.SyncLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow

private const val TABLE = "transactions"

// `open` for one reason: [fetchRemotePage] is the only seam through which a test can drive the
// REAL pull — this class's own applyRemoteRow, the shared page loop, and a real SQLite database —
// with a chosen page of remote rows. A fake table sync instead would have to re-implement the very
// branch under test, which is how a green suite once coexisted with a broken write path here.
open class TransactionTableSync(private val db: EmmDatabaseData, client: SupabaseClient, logger: SyncLogger) :
    BaseTableSync<TransactionRowDto>(
        client = client,
        transact = { body -> db.transaction { body() } },
        logger = logger,
        tableName = TABLE,
    ) {

    // ---------------------------------------------------------------------------
    // Push hooks
    // ---------------------------------------------------------------------------

    override suspend fun selectPendingDtos(userId: String): List<TransactionRowDto> {
        val pending = safeDbCall { db.transactionsQueries.selectPending().executeAsList() }
        // Defence-in-depth: only push rows owned by the signed-in user. selectPending already
        // filters userId IS NOT NULL, but a row claimed by a different account must never leak.
        return pending
            .filter { it.userId == userId }
            .mapNotNull { row ->
                // A row whose stored occurredAt does not parse is already invisible on every
                // screen (the mappers drop it); pushing an invented date for it would make the
                // damage spread to the other devices. It stays Pending and stays local.
                val occurredAt = row.occurredAt.toOccurredAtOrNull()
                if (occurredAt == null) {
                    logger.warn("push skipped unreadable occurredAt table=$TABLE pk=${row.transactionId}")
                    return@mapNotNull null
                }
                TransactionRowDto(
                    transactionId = row.transactionId,
                    type = row.type,
                    amount = row.amount,
                    description = row.description,
                    date = occurredAt.toFixedPeruEpochMillis(),
                    categoryId = row.categoryId,
                    accountId = row.accountId,
                    createdAt = row.createdAt,
                    updatedAt = row.updatedAt,
                    userId = row.userId,
                    deletedAt = row.deletedAt,
                )
            }
    }

    override fun pkOf(dto: TransactionRowDto): String = dto.transactionId

    override fun markSynced(pk: String, updatedAt: Long) {
        db.transactionsQueries.markSynced(pk, updatedAt)
    }

    override suspend fun upsertDtos(dtos: List<TransactionRowDto>) {
        // Composite PK (user_id, transaction_id) on the remote: the conflict target MUST match so
        // the upsert merges on this user's own row instead of colliding with another tenant's id
        // and failing the RLS check. Default ignoreDuplicates=false keeps resolution=merge-duplicates.
        client.postgrest.from(TABLE).upsert(dtos) { onConflict = "user_id,transaction_id" }
    }

    // ---------------------------------------------------------------------------
    // Pull hooks
    // ---------------------------------------------------------------------------

    override val pkColumn: String = "transaction_id"

    override suspend fun fetchRemotePage(
        userId: String,
        overlapCursor: String?,
        after: PullPageKey?,
        limit: Int,
    ): List<TransactionRowDto> = client.postgrest.from(TABLE).select {
        filter { applyPageFilter(userId, overlapCursor, after, pkColumn) }
        order("server_updated_at", Order.ASCENDING)
        order(pkColumn, Order.ASCENDING)
        limit(limit.toLong())
    }.decodeList()

    override fun localRevision(pk: String): LocalRevision? = db.transactionsQueries
        .transactionSyncRevision(pk)
        .executeAsOneOrNull()
        ?.let { localRevisionOf(it.updatedAt, it.syncState) }

    /**
     * Two-statement upsert: INSERT OR IGNORE handles new rows; UPDATE handles existing ones —
     * neither triggers an implicit DELETE, so child-table FK constraints (ON DELETE RESTRICT/SET NULL)
     * are safe.
     *
     * Two things can stop a row landing, and they are NOT the same thing:
     *  - an FK constraint failure — the parent account/category has not been pulled/pushed yet, so
     *    the row is [RemoteRowOutcome.Deferred] and holds the cursor until the parent arrives;
     *  - a remote `date` outside the range a local datetime can represent — that row is
     *    [RemoteRowOutcome.Dropped], because re-reading it next cycle produces the same answer.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override fun applyRemoteRow(remote: TransactionRowDto): RemoteRowOutcome {
        // Untrusted input: the wire still carries an instant, and this client has already had to
        // survive a server timestamp it could not read.
        //
        // Dropped, not deferred. Retrying an unreadable value is not a retry, it is a freeze: a
        // held cursor is shared across all four tables, so one row this client can never parse
        // would stop every remote change from ever reaching the device again — silently, and
        // logged as an FK miss, which is not what happened. Clamping it to a sentinel is worse
        // still: an invented occurredAt corrupts the lexicographic ordering the table depends on.
        // So the row stays local-nothing and the pull moves past it. If the server ever corrects
        // it, `server_updated_at` bumps and it comes back on its own.
        val occurredAt = localDateTimeFromFixedPeru(remote.date)
        if (occurredAt == null) {
            logger.warn("pull dropped unreadable date table=$TABLE pk=${remote.transactionId}")
            return RemoteRowOutcome.Dropped
        }
        val occurredAtText = occurredAt.toOccurredAtText()
        val categoryId = categoryIdFor(remote.categoryId, remote.type, remote.transactionId)
        return try {
            db.transactionsQueries.insertOrIgnoreFromRemote(
                transactionId = remote.transactionId,
                type = remote.type,
                amount = remote.amount,
                description = remote.description,
                occurredAt = occurredAtText,
                categoryId = categoryId,
                accountId = remote.accountId,
                createdAt = remote.createdAt,
                updatedAt = remote.updatedAt,
                userId = remote.userId,
                deletedAt = remote.deletedAt,
            )
            db.transactionsQueries.updateFromRemote(
                type = remote.type,
                amount = remote.amount,
                description = remote.description,
                occurredAt = occurredAtText,
                categoryId = categoryId,
                accountId = remote.accountId,
                createdAt = remote.createdAt,
                updatedAt = remote.updatedAt,
                userId = remote.userId,
                deletedAt = remote.deletedAt,
                transactionId = remote.transactionId,
            )
            RemoteRowOutcome.Applied
        } catch (e: Exception) {
            if (e.isSqliteConstraintViolation()) RemoteRowOutcome.Deferred else throw e
        }
    }

    /**
     * The category id this remote row may be written with, given the composite key
     * `(categoryId, type)`.
     *
     * Three answers, and the middle one is the whole point of asking instead of letting the key
     * fire:
     *  - the category is **absent locally** — pass the id through UNCHANGED. The key then refuses
     *    the row, [applyRemoteRow] reports [RemoteRowOutcome.Deferred], and the cursor is held until
     *    the parent arrives. That is the pre-existing FK-miss contract and it must not be traded
     *    away for a null;
     *  - the category **exists with the other type** — write the row uncategorized. Categories are
     *    pulled before transactions in the same cycle, so the parent is already current: this is a
     *    genuine mismatch, not a race, and deferring it would hold the shared cursor forever;
     *  - the types **agree** — pass it through.
     *
     * A row with no category at all is the fourth, silent case, and it needs no branch: the lookup
     * is null-safe and the pass-through returns the null it was given.
     *
     * Remote rows written before the composite key existed are the reason the middle case is not
     * hypothetical: the server holds whatever pairs this device pushed while nothing checked them.
     */
    private fun categoryIdFor(categoryId: String?, type: String, pk: String): String? {
        val storedType = categoryId?.let { db.categoriesQueries.typeOf(it).executeAsOneOrNull() }
        if (storedType == null || storedType == type) return categoryId
        logger.warn("pull dropped category of the wrong type table=$TABLE pk=$pk")
        return null
    }

    override fun markPendingForResync(pk: String) {
        db.transactionsQueries.markPendingForResync(pk)
    }

    // Wrapped like every other observe flow in this module: this one feeds the orchestrator's
    // debounced-write trigger, so an unwrapped SQLite throw would escape into an application-scope
    // coroutine instead of arriving as a DomainException.
    override fun pendingCount(): Flow<Long> = db.transactionsQueries.countPending()
        .asFlow()
        .mapToOne(ioDispatcher)
        .catchAsDomainException()
}
