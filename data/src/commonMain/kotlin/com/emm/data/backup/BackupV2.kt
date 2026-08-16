// The file is named for the schema version it freezes, not for the single type it declares —
// `ExportPayloadV2Dto` and `BackupV2.kt` intentionally do not match.
@file:Suppress("MatchingDeclarationName")

package com.emm.data.backup

import kotlinx.serialization.Serializable

@Serializable
internal data class ExportPayloadV2Dto(
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION_V2,
    val exportedAt: Long,
    val appVersion: String,
    val accounts: List<AccountDto>,
    val categories: List<CategoryDto>,
    val transactions: List<TransactionDto>,
)

internal const val BACKUP_SCHEMA_VERSION_V2: Int = 2

internal fun ExportPayloadV2Dto.toCurrent(): ExportPayloadDto = ExportPayloadDto(
    schemaVersion = BACKUP_SCHEMA_VERSION,
    exportedAt = exportedAt,
    appVersion = appVersion,
    accounts = accounts,
    categories = categories,
    transactions = transactions,
    recurringMovements = emptyList(),
)
