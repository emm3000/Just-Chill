package com.emm.data.sync

import android.database.sqlite.SQLiteConstraintException
import com.emm.data.EmmDatabaseData
import com.emm.data.shared.safeDbCall
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

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
        client.postgrest.from(TABLE).upsert(dtos)
    }

    // ---------------------------------------------------------------------------
    // Pull hooks
    // ---------------------------------------------------------------------------

    override suspend fun fetchRemoteRows(userId: String, overlapCursor: String?): List<AccountRowDto> =
        if (overlapCursor != null) {
            client.postgrest.from(TABLE).select {
                filter {
                    eq("user_id", userId)
                    gte("server_updated_at", overlapCursor)
                }
            }.decodeList()
        } else {
            client.postgrest.from(TABLE).select {
                filter { eq("user_id", userId) }
            }.decodeList()
        }

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
    } catch (e: SQLiteConstraintException) {
        // Orphan FK: a referenced parent has not been pulled/pushed yet. Skip this row only;
        // other rows in the transaction still apply. The held cursor lets it retry next cycle.
        false
    }

    override fun markPendingForResync(pk: String) {
        db.accountsQueries.markPendingForResync(pk)
    }
}
