package com.emm.domain.report

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money

data class CategoryAggregate(
    val categoryId: CategoryId,
    val categoryName: String,
    val categoryColor: String,
    val categoryIcon: String,
    val totalAmount: Money,
    val monthsInTop: Int,
    val totalMonths: Int,
)
