package com.emm.data.backup

import kotlinx.serialization.Serializable

/**
 * Schema version 2: a transaction carries `occurredAt` (ISO local text) where version 1 carried
 * `date` (epoch millis).
 *
 * The version is written by export and read by import; see `DefaultBackupRepository.importFromJson`
 * for which versions still restore.
 */
const val BACKUP_SCHEMA_VERSION: Int = 2

@Serializable
data class ExportPayloadDto(
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
    val exportedAt: Long,
    val appVersion: String,
    val accounts: List<AccountDto>,
    val categories: List<CategoryDto>,
    val transactions: List<TransactionDto>,
)
