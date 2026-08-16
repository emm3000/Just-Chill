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
