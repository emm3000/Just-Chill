package com.emm.domain.sync

/**
 * Pure Last-Write-Wins (LWW) conflict resolver.
 *
 * Compares client-written `updatedAt` epoch-millis timestamps. The server's
 * `server_updated_at` is used only for pull ordering (ADR 002) — it never enters
 * this class. Tombstones are NOT special-cased: a soft-deleted row wins or loses
 * by the same timestamp comparison as any other mutation.
 *
 * Tie-break: equal timestamps → ApplyRemote (deterministic remote-wins, per PLAN).
 */
class ConflictResolver {

    /**
     * @param localUpdatedAt  Client `updatedAt` of the local row, or `null` if no local row exists.
     * @param remoteUpdatedAt Client `updatedAt` of the incoming remote row.
     */
    fun resolve(localUpdatedAt: Long?, remoteUpdatedAt: Long): Resolution {
        if (localUpdatedAt == null) return Resolution.ApplyRemote
        return if (localUpdatedAt > remoteUpdatedAt) Resolution.KeepLocal else Resolution.ApplyRemote
    }
}

enum class Resolution { ApplyRemote, KeepLocal }
