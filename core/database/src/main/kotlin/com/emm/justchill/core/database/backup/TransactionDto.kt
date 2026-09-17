package com.emm.justchill.core.database.backup

import com.emm.justchill.core.database.shared.enumValueOrNull
import com.emm.justchill.core.database.shared.toOccurredAtOrNull
import com.emm.justchill.core.database.shared.toOccurredAtText
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.TransactionId
import com.emm.justchill.core.domain.transaction.Transaction
import com.emm.justchill.core.domain.transaction.TransactionType
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val transactionId: String,
    val type: String,
    val amountCents: Long,
    val description: String,
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
