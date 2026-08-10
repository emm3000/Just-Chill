package com.emm.justchill.hh.report

import com.emm.domain.report.CategoryAggregate
import com.emm.domain.report.CategoryAmount
import com.emm.domain.shared.Money
import com.emm.justchill.hh.shared.UiStrings

// ── Mapping helpers ───────────────────────────────────────────────────

internal fun buildShares(amounts: List<CategoryAmount>, total: Money): List<CategoryShare> {
    if (total.cents == 0L) return amounts.map { it.toCategoryShare(percentage = 0) }
    return amounts.map { item ->
        val pct = ((item.amount.cents.toDouble() / total.cents.toDouble()) * 100).toInt()
        item.toCategoryShare(percentage = pct)
    }
}

internal fun CategoryAmount.toCategoryShare(percentage: Int) = CategoryShare(
    categoryId = categoryId?.value,
    name = categoryName ?: UiStrings.UNCATEGORIZED,
    amountFormatted = formatSoles(amount.cents),
    percentage = percentage,
    colorKey = categoryColor,
)

internal fun CategoryAggregate.toTopCategoryItem(): TopCategoryItem = TopCategoryItem(
    categoryId = categoryId.value,
    name = categoryName,
    iconKey = categoryIcon,
    colorKey = categoryColor,
    totalFormatted = formatSoles(totalAmount.cents),
    topMetaText = ReportShareFormatter.buildTopMetaText(monthsInTop, totalMonths),
)
