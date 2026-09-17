package com.emm.justchill.core.database.transaction

data class MonthlyAmountByTypeEntity(
    val type: String,
    val categoryId: String?,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColor: String?,
    val totalAmount: Long,
)
