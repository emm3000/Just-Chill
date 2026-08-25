package com.emm.data.transaction

import com.emm.data.Transactions
import com.emm.data.shared.enumValueOrNull
import com.emm.data.shared.toOccurredAtOrNull
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionTotals
import com.emm.domain.transaction.TransactionType

fun Transactions.asEntity() = TransactionEntity(
    transactionId = transactionId,
    type = type,
    amount = amount,
    description = description,
    occurredAt = occurredAt,
    categoryId = categoryId,
    accountId = accountId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun List<Transactions>.asEntity() = map(Transactions::asEntity)

fun TransactionEntity.asExternalModelOrNull(): Transaction? {
    val parsedType = enumValueOrNull<TransactionType>(type)
    val parsedOccurredAt = occurredAt.toOccurredAtOrNull()
    return if (parsedType == null || parsedOccurredAt == null) {
        null
    } else {
        Transaction(
            transactionId = TransactionId(transactionId),
            type = parsedType,
            amount = Money(cents = amount),
            description = description,
            occurredAt = parsedOccurredAt,
            categoryId = categoryId?.let(::CategoryId),
            accountId = AccountId(accountId),
        )
    }
}

fun List<TransactionEntity>.asExternalModel() = mapNotNull(TransactionEntity::asExternalModelOrNull)

fun TransactionTotalsEntity.toDomain() = TransactionTotals(
    balance = Money(cents = balance),
    movementCount = movementCount,
)

fun List<CategoryUsageCountEntity>.toDomain(): Map<CategoryId, Int> =
    associate { CategoryId(it.categoryId) to it.usageCount.toInt() }
