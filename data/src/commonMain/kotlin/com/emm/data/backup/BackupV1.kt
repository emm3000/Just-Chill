package com.emm.data.backup

import com.emm.data.shared.localDateTimeFromFixedPeru
import com.emm.data.shared.toOccurredAtText
import kotlinx.serialization.Serializable

/**
 * The backup format as it was before `occurredAt` existed: a transaction carried `date`, epoch
 * millis.
 *
 * **This has to keep working forever.** Backup files are the data escape hatch and they live in
 * storage the user chose, outside the app, beyond the reach of any schema migration. A file
 * written last month is not obsolete just because the model moved on — and the failure mode if it
 * stops loading is the ugliest kind: `TransactionDto.occurredAt` has no default, so an old file
 * fails deserialization and surfaces as *"El archivo está dañado o no es un respaldo de
 * JustChill"* — telling the user their perfectly good file is corrupt.
 *
 * It is a separate type rather than optional fields on the current DTO on purpose. A v1 file must
 * be read by the shape v1 actually had, frozen here, so that changing the current DTO cannot
 * quietly change how old files are interpreted. `BackupV1CompatibilityTest` pins it against a JSON
 * fixture written out by hand for the same reason.
 *
 * Only the transaction changed between the two versions; accounts and categories are shared.
 */
@Serializable
internal data class ExportPayloadV1Dto(
    /**
     * Defaulted exactly as v1's own DTO defaulted it. A file with no `schemaVersion` key is a v1
     * file — the field only started being written when there was a second version to distinguish —
     * and this default is what lets that file deserialize instead of failing on a missing field.
     */
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION_V1,
    val exportedAt: Long,
    val appVersion: String,
    val accounts: List<AccountDto>,
    val categories: List<CategoryDto>,
    val transactions: List<TransactionV1Dto>,
)

@Serializable
internal data class TransactionV1Dto(
    val transactionId: String,
    val type: String,
    val amountCents: Long,
    val description: String,
    val date: Long,
    val accountId: String,
    val categoryId: String?,
)

internal const val BACKUP_SCHEMA_VERSION_V1: Int = 1

/**
 * Reads a v1 payload as the current one.
 *
 * The instant becomes a local datetime at the same fixed America/Lima offset the 3 → 4 schema
 * migration uses — one assumption, made once, in one place ([localDateTimeFromFixedPeru]), so a
 * row restored from a file and the same row migrated in place land on the same value.
 *
 * A `date` that offset cannot represent drops its row rather than restoring at an invented time.
 * The rest of the file still restores: one unreadable movement is not a reason to refuse a backup.
 */
internal fun ExportPayloadV1Dto.toCurrent(): ExportPayloadDto = ExportPayloadDto(
    schemaVersion = BACKUP_SCHEMA_VERSION,
    exportedAt = exportedAt,
    appVersion = appVersion,
    accounts = accounts,
    categories = categories,
    transactions = transactions.mapNotNull { dto ->
        val occurredAt = localDateTimeFromFixedPeru(dto.date) ?: return@mapNotNull null
        TransactionDto(
            transactionId = dto.transactionId,
            type = dto.type,
            amountCents = dto.amountCents,
            description = dto.description,
            occurredAt = occurredAt.toOccurredAtText(),
            accountId = dto.accountId,
            categoryId = dto.categoryId,
        )
    },
)
