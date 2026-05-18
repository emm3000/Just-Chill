package com.emm.data.transaction

data class MonthlyAmountByCategoryEntity(
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val totalAmount: Long,  // SUM is nullable in SQL but GROUP BY ensures at least one row
)
