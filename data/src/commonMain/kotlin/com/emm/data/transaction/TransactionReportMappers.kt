package com.emm.data.transaction

import com.emm.data.MonthlyAmountByCategory
import com.emm.data.MonthlyAmountByCategoryAndType
import com.emm.data.shared.enumValueOrNull
import com.emm.domain.report.CategoryAmount
import com.emm.domain.report.MonthCategoryAmounts
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.transaction.TransactionType

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

fun List<MonthlyAmountByTypeEntity>.toMonthCategoryAmounts(): MonthCategoryAmounts {
    val byType: Map<TransactionType, List<CategoryAmount>> = groupBy { enumValueOrNull<TransactionType>(it.type) }
        .mapNotNull { (type, rows) -> type?.let { it to rows.map(MonthlyAmountByTypeEntity::toDomain) } }
        .toMap()
    return MonthCategoryAmounts(
        income = byType[TransactionType.Income].orEmpty(),
        expense = byType[TransactionType.Spend].orEmpty(),
    )
}
