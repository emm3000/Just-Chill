package com.emm.data.transaction

// Category columns are nullable because the query LEFT JOINs: rows with no live category
// collapse into a single bucket whose category columns are all null.
data class MonthlyAmountByCategoryEntity(
    val categoryId: String?,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColor: String?,
    val totalAmount: Long, // SUM is nullable in SQL but GROUP BY ensures at least one row
)
