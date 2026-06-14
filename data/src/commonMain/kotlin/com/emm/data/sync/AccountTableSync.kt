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

private const val TABLE = "accounts"

class AccountTableSync(private val db: EmmDatabaseData, client: SupabaseClient) :
    BaseTableSync<AccountRowDto>(
        client = client,
        transact = { body -> db.transaction { body() } },
    ) {

    // ---------------------------------------------------------------------------
    // Push hooks
    // ---------------------------------------------------------------------------

    override suspend fun selectPendingDtos(userId: String): List<AccountRowDto> {
        val pending = safeDbCall { db.accountsQueries.selectPending().executeAsList() }
        // Defence-in-depth: only push rows owned by the signed-in user. selectPending already
        // filters userId IS NOT NULL, but a row claimed by a different account must never leak.
        return pending
            .filter { it.userId == userId }
            .map { row ->
                AccountRowDto(
                    accountId = row.accountId,
                    name = row.name,
                    type = row.type,
                    currency = row.currency,
                    updatedAt = row.updatedAt,
                    createdAt = row.createdAt,
                    userId = row.userId, // non-null: selectPending filters userId IS NOT NULL
                    deletedAt = row.deletedAt,
                )
            }
    }

    override fun pkOf(dto: AccountRowDto): String = dto.accountId

    override fun markSynced(pk: String, updatedAt: Long) {
        db.accountsQueries.markSynced(pk, updatedAt)
    }

    override suspend fun upsertDtos(dtos: List<AccountRowDto>) {
        // Composite PK (user_id, account_id) on the remote: the conflict target MUST match so the
        // upsert merges on this user's own row instead of colliding with another tenant's id and
        // failing the RLS check. Default ignoreDuplicates=false keeps resolution=merge-duplicates.
        client.postgrest.from(TABLE).upsert(dtos) { onConflict = "user_id,account_id" }
    }

    // ---------------------------------------------------------------------------
    // Pull hooks
    // ---------------------------------------------------------------------------

    override val pkColumn: String = "account_id"

    override suspend fun fetchRemotePage(
        userId: String,
        overlapCursor: String?,
        after: PullPageKey?,
        limit: Int,
    ): List<AccountRowDto> = client.postgrest.from(TABLE).select {
        filter { applyPageFilter(userId, overlapCursor, after, pkColumn) }
        order("server_updated_at", Order.ASCENDING)
        order(pkColumn, Order.ASCENDING)
        limit(limit.toLong())
    }.decodeList()

    override fun localUpdatedAt(pk: String): Long? = db.accountsQueries.findForSync(pk).executeAsOneOrNull()

    /**
     * Two-statement upsert: INSERT OR IGNORE handles new rows; UPDATE handles existing ones —
     * neither triggers an implicit DELETE, so child-table FK constraints (ON DELETE RESTRICT/SET NULL)
     * are safe. Returns false if the row was skipped because of an FK constraint failure (a parent
     * row absent locally — it will retry next cycle once the parent arrives).
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override fun applyRemoteRow(remote: AccountRowDto): Boolean = try {
        db.accountsQueries.insertOrIgnoreFromRemote(
            accountId = remote.accountId,
            name = remote.name,
            type = remote.type,
            currency = remote.currency,
            updatedAt = remote.updatedAt,
            createdAt = remote.createdAt,
            userId = remote.userId,
            deletedAt = remote.deletedAt,
        )
        db.accountsQueries.updateFromRemote(
            name = remote.name,
            type = remote.type,
            currency = remote.currency,
            updatedAt = remote.updatedAt,
            createdAt = remote.createdAt,
            userId = remote.userId,
            deletedAt = remote.deletedAt,
            accountId = remote.accountId,
        )
        true
    } catch (e: Exception) {
        if (e.isSqliteConstraintViolation()) false else throw e
    }

    override fun markPendingForResync(pk: String) {
        db.accountsQueries.markPendingForResync(pk)
    }

    override fun pendingCount(): Flow<Long> = db.accountsQueries.countPending().asFlow().mapToOne(ioDispatcher)
}
