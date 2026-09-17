package com.emm.data.transaction

import com.emm.data.CompleteTransactions
import com.emm.data.CompleteTransactionsByDateRange
import com.emm.data.SearchTransactions
import com.emm.data.shared.enumValueOrNull
import com.emm.data.shared.toOccurredAtOrNull
import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.TransactionId
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.domain.transaction.TransactionWithCategory

fun CompleteTransactions.asEntity() = TransactionWithCategoryEntity(
    transactionId = transactionId,
    type = type,
    amount = amount,
    description = description,
    occurredAt = occurredAt,
    accountId = accountId,
    accountName = accountName,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColor = categoryColor,
    categoryType = categoryType_,
)

fun CompleteTransactionsByDateRange.asEntity() = TransactionWithCategoryEntity(
    transactionId = transactionId,
    type = type,
    amount = amount,
    description = description,
    occurredAt = occurredAt,
    accountId = accountId,
    accountName = accountName,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColor = categoryColor,
    categoryType = categoryType_,
)

fun SearchTransactions.asEntity() = TransactionWithCategoryEntity(
    transactionId = transactionId,
    type = type,
    amount = amount,
    description = description,
    occurredAt = occurredAt,
    accountId = accountId,
    accountName = accountName,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColor = categoryColor,
    categoryType = categoryType_,
)

fun TransactionWithCategoryEntity.toDomainOrNull(): TransactionWithCategory? {
    val parsedType = enumValueOrNull<TransactionType>(type)
    val parsedOccurredAt = occurredAt.toOccurredAtOrNull()
    return if (parsedType == null || parsedOccurredAt == null) {
        null
    } else {
        TransactionWithCategory(
            transactionId = TransactionId(transactionId),
            type = parsedType,
            amount = Money(cents = amount),
            description = description,
            occurredAt = parsedOccurredAt,
            accountId = AccountId(accountId),
            accountName = accountName.orEmpty(),
            category = resolveCategory(),
        )
    }
}

private fun TransactionWithCategoryEntity.resolveCategory(): Category? {
    val parsedType = categoryType?.let { enumValueOrNull<CategoryType>(it) } ?: return null
    val allFieldsPresent = categoryId != null && categoryName != null && categoryIcon != null && categoryColor != null
    return if (allFieldsPresent) {
        Category(
            categoryId = CategoryId(categoryId),
            name = categoryName,
            icon = categoryIcon,
            color = categoryColor,
            categoryType = parsedType,
        )
    } else {
        null
    }
}

fun List<TransactionWithCategoryEntity>.toDomain() = mapNotNull(TransactionWithCategoryEntity::toDomainOrNull)
