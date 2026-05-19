package com.emm.domain.shared.backup

interface BackupRepository {

    suspend fun exportToJson(exportedAt: Long, appVersion: String): String

    suspend fun importFromJson(json: String): ImportStats
}
