package com.emm.justchill.core.domain.report

import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money

data class CategoryAmount(
    val categoryId: CategoryId?,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColor: String?,
    val amount: Money,
)
