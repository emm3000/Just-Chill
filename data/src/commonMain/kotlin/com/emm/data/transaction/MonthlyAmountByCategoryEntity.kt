package com.emm.data.transaction

data class MonthlyAmountByCategoryEntity(
    val categoryId: String?,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColor: String?,
    val totalAmount: Long,
)
