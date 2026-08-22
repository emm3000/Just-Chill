// The file is named for the schema version it freezes, not for the single type it declares —
// `ExportPayloadV3Dto` and `BackupV3.kt` intentionally do not match.
@file:Suppress("MatchingDeclarationName")

package com.emm.data.backup

import kotlinx.serialization.Serializable

@Serializable
internal data class ExportPayloadV3Dto(
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION_V3,
    val exportedAt: Long,
    val appVersion: String,
    val accounts: List<AccountDto>,
    val categories: List<CategoryDto>,
    val transactions: List<TransactionDto>,
    val recurringMovements: List<RecurringMovementDto>,
)

internal const val BACKUP_SCHEMA_VERSION_V3: Int = 3

internal fun ExportPayloadV3Dto.toCurrent(): ExportPayloadDto = ExportPayloadDto(
    schemaVersion = BACKUP_SCHEMA_VERSION,
    exportedAt = exportedAt,
    appVersion = appVersion,
    accounts = accounts,
    categories = categories,
    transactions = transactions,
    recurringMovements = recurringMovements,
    loans = emptyList(),
    loanPayments = emptyList(),
)
