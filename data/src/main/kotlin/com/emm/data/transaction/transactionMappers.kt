package com.emm.data.transaction

import com.emm.data.CompleteTransactions
import com.emm.data.Transactions
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.TransactionId
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionWithCategory

// SQLDelight -> Entity (internal, stays within data source)
fun Transactions.asEntity() = TransactionEntity(
    transactionId = transactionId,
    type = type,
    amount = amount,
    description = description,
    date = date,
    categoryId = categoryId,
    accountId = accountId,
    syncState = syncState,
    isDeleted = isDeleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun List<Transactions>.asEntity() = map(Transactions::asEntity)

// Entity -> Domain
fun TransactionEntity.asExternalModel() = Transaction(
    transactionId = TransactionId(transactionId),
    type = TransactionType.valueOf(type),
    amount = amount,
    description = description,
    date = date,
    categoryId = categoryId?.let(::CategoryId),
    accountId = AccountId(accountId),
)

fun List<TransactionEntity>.asExternalModel() = map(TransactionEntity::asExternalModel)

// Domain insert -> Entity
fun TransactionInsert.asEntity() = TransactionEntity(
    transactionId = id.value,
    type = type.name,
    amount = amount,
    description = description,
    date = date,
    categoryId = categoryId?.value,
    accountId = accountId.value,
    syncState = "",
    isDeleted = false,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// Network -> Entity
fun NetworkTransaction.asEntity() = TransactionEntity(
    transactionId = transactionId,
    type = type,
    amount = amount,
    description = description,
    date = date,
    categoryId = categoryId,
    accountId = accountId,
    syncState = "",
    isDeleted = deleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// SQLDelight CompleteTransactions -> Entity
fun CompleteTransactions.asEntity() = TransactionWithCategoryEntity(
    transactionId = transactionId,
    type = type,
    amount = amount,
    description = description,
    date = date,
    accountId = accountId,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColor = categoryColor,
    categoryType = categoryType_,
)

// TransactionWithCategoryEntity -> Domain
fun TransactionWithCategoryEntity.toDomain() = TransactionWithCategory(
    transactionId = TransactionId(transactionId),
    type = TransactionType.valueOf(type),
    amount = amount,
    description = description,
    date = date,
    accountId = AccountId(accountId),
    category = if (categoryId != null && categoryName != null && categoryIcon != null && categoryColor != null && categoryType != null) {
        Category(
            categoryId = CategoryId(categoryId),
            name = categoryName,
            icon = categoryIcon,
            color = categoryColor,
            categoryType = CategoryType.valueOf(categoryType),
        )
    } else null,
)

fun List<TransactionWithCategoryEntity>.toDomain() = map(TransactionWithCategoryEntity::toDomain)

// Entity -> Network
fun TransactionEntity.asNetworkModel(userId: String) = NetworkTransaction(
    transactionId = transactionId,
    type = type,
    amount = amount,
    description = description,
    date = date,
    updatedAt = updatedAt,
    createdAt = createdAt,
    deleted = isDeleted,
    categoryId = categoryId,
    accountId = accountId,
    userId = userId,
)
