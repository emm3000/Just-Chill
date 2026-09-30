package com.emm.justchill.feature.report

import com.emm.justchill.core.presentation.format.formatNeutral

data class TrendsUiData(
    val savingsRatePercent: Int = 0,
    val deltaText: String? = null,
    val deltaIsPositive: Boolean? = null,
    val contextSentence: String = "",
    val monthlyBars: List<MonthlyBarItem> = emptyList(),
    val averageIncomeFormatted: String = formatNeutral("0"),
    val averageExpenseFormatted: String = formatNeutral("0"),
    val topExpenses: List<TopCategoryItem> = emptyList(),
    val isEarlyState: Boolean = false,
) {

    val isSavingsRateDeficit: Boolean
        get() = savingsRatePercent < 0
}

data class MonthlyBarItem(
    val monthShortLabel: String,
    val isCurrentMonth: Boolean,
    val incomeAmount: Long,
    val expenseAmount: Long,
    val incomeFormatted: String,
    val expenseFormatted: String,
    val incomeFraction: Float = 0f,
    val expenseFraction: Float = 0f,
)

data class TopCategoryItem(
    val categoryId: String,
    val name: String,
    val iconKey: String,
    val totalFormatted: String,
    val topMetaText: String,
)
