package com.emm.domain.sync

/**
 * What the local replica holds for a row the pull just delivered.
 *
 * [hasUnpushedEdit] mirrors the local `syncState`: true while the user's change is still waiting
 * to reach the server.
 */
data class LocalRevision(val updatedAt: Long, val hasUnpushedEdit: Boolean)

/**
 * Last-Write-Wins (LWW) conflict resolver, applied only where a conflict actually exists.
 *
 * A row whose local copy is already synced has no local change to protect, so the server copy is
 * authoritative and no clock is consulted. Comparing timestamps there was the bug: a device whose
 * clock ran ahead stamped every row it touched into the future, then kept beating the other
 * device's genuinely newer edit, re-pushed its own stale copy, and overwrote that edit again on
 * every sync cycle. The loss was silent and permanent.
 *
 * Client `updatedAt` still decides when both replicas hold an edit, because that case has no
 * clock-free answer: the local edit has never reached the server, so no server timestamp exists to
 * order it against the remote row. `server_updated_at` orders pulls only (ADR 002) and never
 * enters this class.
 *
 * Tombstones are NOT special-cased: a soft-deleted row wins or loses by the same rules.
 *
 * Tie-break: equal timestamps → ApplyRemote (deterministic remote-wins, per PLAN).
 */
class ConflictResolver {

    /**
     * @param local           The local row, or `null` when the pull delivered a row this device
     *                        has never seen.
     * @param remoteUpdatedAt Client `updatedAt` of the incoming remote row.
     */
    fun resolve(local: LocalRevision?, remoteUpdatedAt: Long): Resolution {
        if (local == null || !local.hasUnpushedEdit) return Resolution.ApplyRemote
        return if (local.updatedAt > remoteUpdatedAt) Resolution.KeepLocal else Resolution.ApplyRemote
    }
}

enum class Resolution { ApplyRemote, KeepLocal }
