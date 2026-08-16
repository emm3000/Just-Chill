package com.emm.domain.shared.backup

interface BackupRepository {

    suspend fun exportToJson(exportedAt: Long, appVersion: String): String

    suspend fun importFromJson(json: String): ImportStats

    /**
     * The epoch-millis `updatedAt` of the most recently changed row across the four tables a
     * backup covers (accounts, categories, transactions, recurring movements) — `null` if none of
     * them holds a single row.
     *
     * Counts soft-deleted rows: a tombstone bumps `updatedAt` the same as any other write, so a
     * deletion is a local change like any other. This is the READ half of "has local data changed
     * since the last backup" — nothing here compares it against the last-backup timestamp or
     * decides whether to trigger anything; that orchestration is a separate concern.
     *
     * Lives on [BackupRepository] rather than a new port: it exists solely to feed the backup
     * subsystem's own decision, and this repository already owns backup data access — a one-method
     * interface beside it would be ceremony with no second implementation to justify it.
     */
    suspend fun latestLocalChangeAt(): Long?
}

/**
 * Has anything changed locally that the last recorded backup does not already hold?
 *
 * The one place that sentence is spelled, because two callers ask it for different purposes and a
 * second spelling would let them disagree on screen: `BackupOrchestrator` asks it to decide whether
 * an automatic cycle is worth a network round trip, and [GetBackupStalenessUseCase] asks it to
 * decide whether the user is warned. A device the orchestrator considers clean must never be one the
 * warning calls dirty.
 *
 * @param latestLocalChangeAt [BackupRepository.latestLocalChangeAt]. A `null` is the four backed-up
 *   tables holding no row at all — structurally distinct from `0`, because SQLDelight types the
 *   column `Long?` — and an empty ledger is nothing to back up.
 * @param lastSuccessfulBackupAt the last verified snapshot's watermark. A `null` is a device that
 *   has never completed one, which is dirty as soon as it holds anything.
 */
fun hasLocalChangesSince(latestLocalChangeAt: Long?, lastSuccessfulBackupAt: Long?): Boolean =
    latestLocalChangeAt != null && (lastSuccessfulBackupAt == null || latestLocalChangeAt > lastSuccessfulBackupAt)
