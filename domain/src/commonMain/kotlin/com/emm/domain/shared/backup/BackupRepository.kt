package com.emm.domain.shared.backup

interface BackupRepository {

    suspend fun exportToJson(exportedAt: Long, appVersion: String): String

    suspend fun importFromJson(json: String): ImportStats

    suspend fun latestLocalChangeAt(): Long?
}

fun hasLocalChangesSince(latestLocalChangeAt: Long?, lastSuccessfulBackupAt: Long?): Boolean =
    latestLocalChangeAt != null && (lastSuccessfulBackupAt == null || latestLocalChangeAt > lastSuccessfulBackupAt)
