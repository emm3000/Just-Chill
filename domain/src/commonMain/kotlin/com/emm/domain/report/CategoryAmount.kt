package com.emm.domain.report

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money

/**
 * Amount spent or earned under one category within a period.
 *
 * A null [categoryId] is the uncategorized bucket: transactions with no category, plus those
 * whose category was deleted. It carries no name, icon or color — the UI supplies the label so
 * that the amounts always add up to the period total.
 */
data class CategoryAmount(
    val categoryId: CategoryId?,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColor: String?,
    val amount: Money,
)
