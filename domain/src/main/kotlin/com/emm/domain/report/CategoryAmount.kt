package com.emm.domain.report

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money

data class CategoryAmount(
    val categoryId: CategoryId?,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColor: String?,
    val amount: Money,
)
