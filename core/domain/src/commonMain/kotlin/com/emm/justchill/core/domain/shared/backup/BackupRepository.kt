package com.emm.justchill.core.domain.shared.backup

import kotlinx.coroutines.flow.Flow

interface BackupRepository {

    suspend fun exportToJson(exportedAt: Long, appVersion: String): String

    suspend fun importFromJson(json: String): ImportStats

    suspend fun latestLocalChangeAt(): Long?

    fun observeLatestLocalChangeAt(): Flow<Long?>
}

fun hasLocalChangesSince(latestLocalChangeAt: Long?, lastSuccessfulBackupAt: Long?): Boolean =
    latestLocalChangeAt != null && (lastSuccessfulBackupAt == null || latestLocalChangeAt > lastSuccessfulBackupAt)
