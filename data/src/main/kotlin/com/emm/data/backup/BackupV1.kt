package com.emm.data.backup

import com.emm.data.shared.localDateTimeFromFixedPeru
import com.emm.data.shared.toOccurredAtText
import kotlinx.serialization.Serializable

@Serializable
internal data class ExportPayloadV1Dto(
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
    recurringMovements = emptyList(),
    loans = emptyList(),
    loanPayments = emptyList(),
)
