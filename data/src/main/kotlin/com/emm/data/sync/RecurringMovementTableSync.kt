package com.emm.data.sync

import android.database.sqlite.SQLiteConstraintException
import com.emm.data.EmmDatabaseData
import com.emm.data.shared.safeDbCall
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

private const val TABLE = "recurring_movements"

class RecurringMovementTableSync(private val db: EmmDatabaseData, client: SupabaseClient) :
    BaseTableSync<RecurringMovementRowDto>(
        client = client,
        transact = { body -> db.transaction { body() } },
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
        client.postgrest.from(TABLE).upsert(dtos)
    }

    // ---------------------------------------------------------------------------
    // Pull hooks
    // ---------------------------------------------------------------------------

    override suspend fun fetchRemoteRows(userId: String, overlapCursor: String?): List<RecurringMovementRowDto> =
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

    override fun localUpdatedAt(pk: String): Long? = db.recurring_movementsQueries.findForSync(pk).executeAsOneOrNull()

    /**
     * Two-statement upsert that never triggers an implicit DELETE.
     * INSERT OR IGNORE inserts new rows; UPDATE patches existing ones in-place.
     * FK constraints (ON DELETE RESTRICT/SET NULL on child tables) are therefore never fired.
     *
     * isActive / dayOfMonth type notes: SQLDelight maps INTEGER columns to Long; the DTO
     * carries Boolean / Int, so we convert before calling the generated queries.
     *
     * Returns false if the row was skipped because of an FK constraint failure (the parent
     * account/category has not been pulled/pushed yet — it will retry next cycle once it arrives).
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override fun applyRemoteRow(remote: RecurringMovementRowDto): Boolean = try {
        // SQLDelight generated INTEGER column; Long parameter expected.
        val dayOfMonthLong = remote.dayOfMonth.toLong()
        // SQLDelight generated INTEGER AS Boolean column needs Long (0/1).
        val isActiveLong = if (remote.isActive) 1L else 0L

        db.recurring_movementsQueries.insertOrIgnoreFromRemote(
            id = remote.id,
            name = remote.name,
            type = remote.type,
            amount = remote.amount,
            description = remote.description,
            categoryId = remote.categoryId,
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
            categoryId = remote.categoryId,
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
        true
    } catch (e: SQLiteConstraintException) {
        // Orphan FK: the referenced account/category has not been pulled/pushed yet. Skip this row
        // only; other rows in the transaction still apply. The held cursor lets it retry next cycle.
        false
    }

    override fun markPendingForResync(pk: String) {
        db.recurring_movementsQueries.markPendingForResync(pk)
    }
}
