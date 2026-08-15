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
