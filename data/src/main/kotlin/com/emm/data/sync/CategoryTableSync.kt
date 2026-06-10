package com.emm.data.sync

import android.database.sqlite.SQLiteConstraintException
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.emm.data.EmmDatabaseData
import com.emm.data.shared.safeDbCall
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

private const val TABLE = "categories"

class CategoryTableSync(private val db: EmmDatabaseData, client: SupabaseClient) :
    BaseTableSync<CategoryRowDto>(
        client = client,
        transact = { body -> db.transaction { body() } },
    ) {

    // ---------------------------------------------------------------------------
    // Push hooks
    // ---------------------------------------------------------------------------

    override suspend fun selectPendingDtos(userId: String): List<CategoryRowDto> {
        val pending = safeDbCall { db.categoriesQueries.selectPending().executeAsList() }
        // Defence-in-depth: only push rows owned by the signed-in user. selectPending already
        // filters userId IS NOT NULL, but a row claimed by a different account must never leak.
        return pending
            .filter { it.userId == userId }
            .map { row ->
                CategoryRowDto(
                    categoryId = row.categoryId,
                    name = row.name,
                    icon = row.icon,
                    color = row.color,
                    categoryType = row.categoryType,
                    isDefault = row.isDefault,
                    updatedAt = row.updatedAt,
                    createdAt = row.createdAt,
                    userId = row.userId,
                    deletedAt = row.deletedAt,
                )
            }
    }

    override fun pkOf(dto: CategoryRowDto): String = dto.categoryId

    override fun markSynced(pk: String, updatedAt: Long) {
        db.categoriesQueries.markSynced(pk, updatedAt)
    }

    override suspend fun upsertDtos(dtos: List<CategoryRowDto>) {
        client.postgrest.from(TABLE).upsert(dtos)
    }

    // ---------------------------------------------------------------------------
    // Pull hooks
    // ---------------------------------------------------------------------------

    override suspend fun fetchRemoteRows(userId: String, overlapCursor: String?): List<CategoryRowDto> =
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

    override fun localUpdatedAt(pk: String): Long? = db.categoriesQueries.findForSync(pk).executeAsOneOrNull()

    /**
     * Two-statement upsert: INSERT OR IGNORE handles new rows; UPDATE handles existing ones —
     * neither triggers an implicit DELETE, so child-table FK constraints (ON DELETE RESTRICT/SET NULL)
     * are safe. Returns false if the row was skipped because of an FK constraint failure (a parent
     * row absent locally — it will retry next cycle once the parent arrives).
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override fun applyRemoteRow(remote: CategoryRowDto): Boolean = try {
        db.categoriesQueries.insertOrIgnoreFromRemote(
            categoryId = remote.categoryId,
            name = remote.name,
            icon = remote.icon,
            color = remote.color,
            categoryType = remote.categoryType,
            isDefault = remote.isDefault,
            updatedAt = remote.updatedAt,
            createdAt = remote.createdAt,
            userId = remote.userId,
            deletedAt = remote.deletedAt,
        )
        db.categoriesQueries.updateFromRemote(
            name = remote.name,
            icon = remote.icon,
            color = remote.color,
            categoryType = remote.categoryType,
            isDefault = remote.isDefault,
            updatedAt = remote.updatedAt,
            createdAt = remote.createdAt,
            userId = remote.userId,
            deletedAt = remote.deletedAt,
            categoryId = remote.categoryId,
        )
        true
    } catch (e: SQLiteConstraintException) {
        // Orphan FK: a referenced parent has not been pulled/pushed yet. Skip this row only;
        // other rows in the transaction still apply. The held cursor lets it retry next cycle.
        false
    }

    override fun markPendingForResync(pk: String) {
        db.categoriesQueries.markPendingForResync(pk)
    }

    override fun pendingCount(): Flow<Long> = db.categoriesQueries.countPending().asFlow().mapToOne(Dispatchers.IO)
}
