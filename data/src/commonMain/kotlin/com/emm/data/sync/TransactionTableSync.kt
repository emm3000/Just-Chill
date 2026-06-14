package com.emm.data.sync

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.emm.data.EmmDatabaseData
import com.emm.data.shared.ioDispatcher
import com.emm.data.shared.isSqliteConstraintViolation
import com.emm.data.shared.safeDbCall
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow

private const val TABLE = "transactions"

class TransactionTableSync(private val db: EmmDatabaseData, client: SupabaseClient) :
    BaseTableSync<TransactionRowDto>(
        client = client,
        transact = { body -> db.transaction { body() } },
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
            .map { row ->
                TransactionRowDto(
                    transactionId = row.transactionId,
                    type = row.type,
                    amount = row.amount,
                    description = row.description,
                    date = row.date,
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

    override fun localUpdatedAt(pk: String): Long? = db.transactionsQueries.findForSync(pk).executeAsOneOrNull()

    /**
     * Two-statement upsert: INSERT OR IGNORE handles new rows; UPDATE handles existing ones —
     * neither triggers an implicit DELETE, so child-table FK constraints (ON DELETE RESTRICT/SET NULL)
     * are safe. Returns false if the row was skipped because of an FK constraint failure (the parent
     * account/category has not been pulled/pushed yet — it will retry next cycle once it arrives).
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override fun applyRemoteRow(remote: TransactionRowDto): Boolean = try {
        db.transactionsQueries.insertOrIgnoreFromRemote(
            transactionId = remote.transactionId,
            type = remote.type,
            amount = remote.amount,
            description = remote.description,
            date = remote.date,
            categoryId = remote.categoryId,
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
            date = remote.date,
            categoryId = remote.categoryId,
            accountId = remote.accountId,
            createdAt = remote.createdAt,
            updatedAt = remote.updatedAt,
            userId = remote.userId,
            deletedAt = remote.deletedAt,
            transactionId = remote.transactionId,
        )
        true
    } catch (e: Exception) {
        if (e.isSqliteConstraintViolation()) false else throw e
    }

    override fun markPendingForResync(pk: String) {
        db.transactionsQueries.markPendingForResync(pk)
    }

    override fun pendingCount(): Flow<Long> = db.transactionsQueries.countPending().asFlow().mapToOne(ioDispatcher)
}
