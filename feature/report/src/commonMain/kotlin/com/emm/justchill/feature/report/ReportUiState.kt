package com.emm.justchill.feature.report

import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.presentation.format.formatNeutral
import com.emm.justchill.core.presentation.mvi.UiState

// isCurrentMonth is computed by the ViewModel, which holds the live calendar month the screen lacks.
data class ReportUiState(
    val month: YearMonth,
    val isCurrentMonth: Boolean = false,
    val selectedType: TransactionType = TransactionType.Spend,
    val selectedTab: ReportTab = ReportTab.Month,
    val totalFormatted: String = formatNeutral("0.00"),
    val comparisonText: String? = null,
    val comparisonDirectionUp: Boolean? = null,
    val comparisonIsPositive: Boolean? = null,
    val comparisonAmountFormatted: String? = null,
    val comparisonPercent: Int = 0,
    val shares: List<CategoryShare> = emptyList(),
    val isEmpty: Boolean = false,
    val isMonthEmpty: Boolean = false,
    val movementCount: Int = 0,
    val averageFormatted: String = formatNeutral("0"),
    val trends: TrendsUiData = TrendsUiData(),
    // ADR 012 Decision 2.
    val showMonthSheet: Boolean = false,
) : UiState {

    val comparisonPillText: String?
        get() = comparisonAmountFormatted?.let { comparisonPillText(it, comparisonPercent) }

    val comparisonPillDescription: String?
        get() {
            val amount: String = comparisonAmountFormatted ?: return null
            val directionUp: Boolean = comparisonDirectionUp ?: return null
            return comparisonPillDescription(amount, comparisonPercent, directionUp)
        }
}
