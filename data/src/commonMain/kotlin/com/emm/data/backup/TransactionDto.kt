package com.emm.data.backup

import com.emm.data.shared.enumValueOrNull
import com.emm.data.shared.toOccurredAtOrNull
import com.emm.data.shared.toOccurredAtText
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionType
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val transactionId: String,
    val type: String,
    val amountCents: Long,
    val description: String,
    /**
     * When the money moved, as ISO local text — the same bytes the column holds, and no timezone.
     *
     * This field replaced `date: Long` in payload schema version 2. A file written by version 1
     * still restores; `BackupV1.kt` owns that conversion.
     */
    val occurredAt: String,
    val accountId: String,
    val categoryId: String?,
)

fun Transaction.toDto() = TransactionDto(
    transactionId = transactionId.value,
    type = type.name,
    amountCents = amount.cents,
    description = description,
    occurredAt = occurredAt.toOccurredAtText(),
    accountId = accountId.value,
    categoryId = categoryId?.value,
)

/**
 * Null when the file carries a row this app cannot interpret — an `occurredAt` that is not a local
 * datetime, or a `type` that is not one of ours. A corrupted or hand-edited row is dropped rather
 * than restored at an invented date or under a guessed type.
 *
 * Null for the type too, not a throw: this runs inside the import's database transaction, and the
 * same skip-the-row policy the storage mappers apply (`TransactionEntity.asExternalModelOrNull`)
 * costs one row, where a throw would abort the whole restore and report the user's file as broken.
 */
fun TransactionDto.toEntityOrNull(): Transaction? {
    val parsedOccurredAt: LocalDateTime? = occurredAt.toOccurredAtOrNull()
    val parsedType: TransactionType? = enumValueOrNull<TransactionType>(type)
    return if (parsedOccurredAt == null || parsedType == null) {
        null
    } else {
        Transaction(
            transactionId = TransactionId(transactionId),
            type = parsedType,
            amount = Money(amountCents),
            description = description,
            occurredAt = parsedOccurredAt,
            accountId = AccountId(accountId),
            categoryId = categoryId?.let { CategoryId(it) },
        )
    }
}
