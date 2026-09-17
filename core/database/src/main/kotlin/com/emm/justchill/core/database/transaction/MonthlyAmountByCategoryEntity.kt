package com.emm.justchill.core.database.transaction

data class MonthlyAmountByCategoryEntity(
    val categoryId: String?,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColor: String?,
    val totalAmount: Long,
)
