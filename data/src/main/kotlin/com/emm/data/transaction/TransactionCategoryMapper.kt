package com.emm.data.transaction

import com.emm.data.CompleteTransactions
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryType
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionWithCategory

fun toDomain(
    transactions: List<CompleteTransactions>
): List<TransactionWithCategory> = transactions.map { completeTransactions ->
    val category: Category? = maybeToCategory(completeTransactions)
    TransactionWithCategory(
        transactionId = completeTransactions.transactionId,
        type = TransactionType.valueOf(completeTransactions.type),
        amount = completeTransactions.amount,
        description = completeTransactions.description,
        date = completeTransactions.date,
        accountId = completeTransactions.accountId,
        category = category,
    )
}

private fun maybeToCategory(
    completeTransactions: CompleteTransactions,
): Category? =
    if (completeTransactions.categoryName != null && completeTransactions.categoryIcon != null && completeTransactions.categoryColor != null) {
        Category(
            categoryId = "-",
            name = completeTransactions.categoryName,
            icon = completeTransactions.categoryIcon,
            color = completeTransactions.categoryColor,
            categoryType = CategoryType.Income
        )
    } else null