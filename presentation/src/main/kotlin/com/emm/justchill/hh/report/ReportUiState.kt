package com.emm.justchill.hh.report

import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiState

/** [isCurrentMonth] is computed by the ViewModel, which holds the live calendar month the screen lacks. */
data class ReportUiState(
    val month: YearMonth,
    val isCurrentMonth: Boolean = false,
    val selectedType: TransactionType = TransactionType.Spend,
    val selectedTab: ReportTab = ReportTab.Month,
    val totalFormatted: String = "S/ 0.00",
    val comparisonText: String? = null,
    val comparisonDirectionUp: Boolean? = null,
    val comparisonIsPositive: Boolean? = null,
    val comparisonAmountFormatted: String? = null,
    val comparisonPercent: Int = 0,
    val shares: List<CategoryShare> = emptyList(),
    val isEmpty: Boolean = false,
    val isMonthEmpty: Boolean = false,
    val movementCount: Int = 0,
    val averageFormatted: String = "S/ 0",
    val trends: TrendsUiData = TrendsUiData(),
    /** Whether the month picker sheet is rendered over this screen (ADR 012 Decision 2). */
    val showMonthSheet: Boolean = false,
) : UiState
