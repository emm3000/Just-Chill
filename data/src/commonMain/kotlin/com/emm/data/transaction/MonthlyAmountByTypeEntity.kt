package com.emm.data.transaction

// Same shape as MonthlyAmountByCategoryEntity plus the raw type column: one query row now covers
// both Income and Spend, so the type has to survive down to the repository that splits them.
data class MonthlyAmountByTypeEntity(
    val type: String,
    val categoryId: String?,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColor: String?,
    val totalAmount: Long,
)
