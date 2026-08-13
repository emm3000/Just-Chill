package com.emm.data.backup

import kotlinx.serialization.Serializable

/**
 * Schema version 2: a transaction carries `occurredAt` (ISO local text) where version 1 carried
 * `date` (epoch millis).
 *
 * The version is written by export and read by import; see `DefaultBackupRepository.importFromJson`
 * for which versions still restore.
 *
 * **This number moves in the same commit that changes the shape below it, never in an earlier one.**
 * Export stamps it into every file it writes, so bumping it ahead of the fields ships files that
 * declare a format they were not written in — and the next version's reader cannot tell them apart
 * from the real thing, because a list the file never had decodes as an empty one rather than as
 * nothing. On a device holding real accumulated data, "the file says there are none" is what
 * destroys the rows.
 *
 * The order that avoids it: freeze the outgoing shape into its own `BackupV<n>.kt` with its own
 * literal constant first (`BackupV1.kt`, `BackupV2.kt`), then add the new fields and move this
 * number together, then wire the frozen branch in `decodePayload`.
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
