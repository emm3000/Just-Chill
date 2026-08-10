package com.emm.justchill.hh.report

data class TrendsUiData(
    val savingsRatePercent: Int = 0,
    val deltaText: String? = null,
    val deltaIsPositive: Boolean? = null,
    val contextSentence: String = "",
    val monthlyBars: List<MonthlyBarItem> = emptyList(),
    val averageIncomeFormatted: String = "S/ 0",
    val averageExpenseFormatted: String = "S/ 0",
    val topExpenses: List<TopCategoryItem> = emptyList(),
    val isEarlyState: Boolean = false,
)

data class MonthlyBarItem(
    val monthShortLabel: String,
    val isCurrentMonth: Boolean,
    val incomeAmount: Long,
    val expenseAmount: Long,
    val incomeFormatted: String,
    val expenseFormatted: String,
)

data class TopCategoryItem(
    val categoryId: String,
    val name: String,
    val iconKey: String,
    /** Domain color string, resolved to a `cat.*` token at render time via `domainColorToUi`. */
    val colorKey: String?,
    val totalFormatted: String,
    val topMetaText: String,
)
