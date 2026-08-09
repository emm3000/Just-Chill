package com.emm.data.transaction

import com.emm.data.CompleteTransactions
import com.emm.data.CompleteTransactionsByDateRange
import com.emm.data.MonthlyAmountByCategory
import com.emm.data.MonthlyAmountByCategoryAndType
import com.emm.data.SearchTransactions
import com.emm.data.Transactions
import com.emm.data.shared.enumValueOrNull
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryType
import com.emm.domain.report.CategoryAmount
import com.emm.domain.report.MonthCategoryAmounts
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionTotals
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionWithCategory

fun Transactions.asEntity() = TransactionEntity(
    transactionId = transactionId,
    type = type,
    amount = amount,
    description = description,
    date = date,
    categoryId = categoryId,
    accountId = accountId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun List<Transactions>.asEntity() = map(Transactions::asEntity)

fun TransactionEntity.asExternalModelOrNull(): Transaction? {
    val parsedType = enumValueOrNull<TransactionType>(type) ?: return null
    return Transaction(
        transactionId = TransactionId(transactionId),
        type = parsedType,
        amount = Money(cents = amount),
        description = description,
        date = date,
        categoryId = categoryId?.let(::CategoryId),
        accountId = AccountId(accountId),
    )
}

fun List<TransactionEntity>.asExternalModel() = mapNotNull(TransactionEntity::asExternalModelOrNull)

fun TransactionInsert.asEntity() = TransactionEntity(
    transactionId = id.value,
    type = type.name,
    amount = amount.cents,
    description = description,
    date = date,
    categoryId = categoryId?.value,
    accountId = accountId.value,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

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

fun CompleteTransactionsByDateRange.asEntity() = TransactionWithCategoryEntity(
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

fun SearchTransactions.asEntity() = TransactionWithCategoryEntity(
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

fun TransactionWithCategoryEntity.toDomainOrNull(): TransactionWithCategory? {
    val parsedType = enumValueOrNull<TransactionType>(type) ?: return null
    return TransactionWithCategory(
        transactionId = TransactionId(transactionId),
        type = parsedType,
        amount = Money(cents = amount),
        description = description,
        date = date,
        accountId = AccountId(accountId),
        category = resolveCategory(),
    )
}

// Unknown categoryType keeps the transaction alive but sets category = null.
// The UI already renders null category as "Sin categoría"; financial totals must not depend on it.
private fun TransactionWithCategoryEntity.resolveCategory(): Category? {
    val parsedType = categoryType?.let { enumValueOrNull<CategoryType>(it) } ?: return null
    val allFieldsPresent = categoryId != null && categoryName != null && categoryIcon != null && categoryColor != null
    return if (allFieldsPresent) {
        Category(
            categoryId = CategoryId(categoryId!!),
            name = categoryName!!,
            icon = categoryIcon!!,
            color = categoryColor!!,
            categoryType = parsedType,
        )
    } else {
        null
    }
}

fun List<TransactionWithCategoryEntity>.toDomain() = mapNotNull(TransactionWithCategoryEntity::toDomainOrNull)

// totalAmount is Long? from SQLDelight (SUM is nullable), default to 0 if null
fun MonthlyAmountByCategory.asEntity() = MonthlyAmountByCategoryEntity(
    categoryId = categoryId,
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColor = categoryColor,
    totalAmount = totalAmount ?: 0L,
)

fun MonthlyAmountByCategoryEntity.toDomain() = CategoryAmount(
    categoryId = categoryId?.let(::CategoryId),
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColor = categoryColor,
    amount = Money(cents = totalAmount),
)

fun MonthlyAmountByCategoryAndType.asEntity() = MonthlyAmountByTypeEntity(
    type = type,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColor = categoryColor,
    totalAmount = totalAmount ?: 0L,
)

fun MonthlyAmountByTypeEntity.toDomain() = CategoryAmount(
    categoryId = categoryId?.let(::CategoryId),
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColor = categoryColor,
    amount = Money(cents = totalAmount),
)

/**
 * Splits one month's rows into the two typed buckets the report reads.
 *
 * Rows whose type the app cannot parse are dropped rather than guessed at — the same policy the
 * transaction mappers apply, so an unreadable row stays invisible everywhere instead of landing in
 * the wrong column of a money screen.
 */
fun List<MonthlyAmountByTypeEntity>.toMonthCategoryAmounts(): MonthCategoryAmounts {
    val byType: Map<TransactionType, List<CategoryAmount>> = groupBy { enumValueOrNull<TransactionType>(it.type) }
        .mapNotNull { (type, rows) -> type?.let { it to rows.map(MonthlyAmountByTypeEntity::toDomain) } }
        .toMap()
    return MonthCategoryAmounts(
        income = byType[TransactionType.Income].orEmpty(),
        expense = byType[TransactionType.Spend].orEmpty(),
    )
}

fun TransactionTotalsEntity.toDomain() = TransactionTotals(
    balance = Money(cents = balance),
    movementCount = movementCount,
)
