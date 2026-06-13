package com.emm.data.sync

import com.emm.domain.sync.ConflictResolver
import kotlinx.coroutines.flow.Flow

/**
 * Per-table sync unit: push locally-pending rows to Supabase, then pull remote changes.
 *
 * Push: read rows with syncState='Pending' AND userId set → keep only rows owned by [userId] →
 *       upsert to remote → markSynced locally.
 * Pull: filter by `user_id = userId` AND `server_updated_at >= cursor - 10s` (ADR 002 overlap window),
 *       LWW-merge each row, idempotent local upsert when ApplyRemote. Returns the max
 *       `server_updated_at` ISO string seen plus whether any rows were skipped.
 */
interface TableSync {
    suspend fun push(userId: String)

    /**
     * @param cursor last pulled-at ISO-8601 UTC string, or null for a full re-pull.
     * @return a [PullResult] carrying the max server_updated_at ISO string seen (null if no rows
     *         pulled) and whether any rows were skipped this cycle (e.g. orphan child rows whose
     *         parent has not arrived yet).
     */
    suspend fun pull(userId: String, cursor: String?, resolver: ConflictResolver): PullResult

    /**
     * Emits the count of rows with syncState = 'Pending' AND userId IS NOT NULL for this table.
     * Used to drive debounced-write sync triggers.
     */
    fun pendingCount(): Flow<Long>
}

/**
 * Outcome of a single table pull.
 *
 * @property maxServerUpdatedAt max `server_updated_at` ISO string applied, or null if no rows pulled.
 * @property skippedRows true if one or more rows were skipped this cycle (deferred to a later pull).
 */
data class PullResult(val maxServerUpdatedAt: String?, val skippedRows: Boolean)
