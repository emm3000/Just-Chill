package com.emm.justchill.hh.report

import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiState

/**
 * [month] carries no default. A default that reads the wall clock is the same defect as an ambient
 * timezone: it answers "which month is it" from the machine, at the moment the class is loaded, for
 * whoever forgot to say. The ViewModel holds the injected clock and zone, so the ViewModel supplies
 * the month — and every other construction site (previews, formatter tests) names one explicitly.
 *
 * [isCurrentMonth] is the answer to "is [month] the month the user is living in", computed by the
 * ViewModel for the same reason: the screen has no injected zone to ask.
 */
data class ReportUiState(
    val month: YearMonth,
    val isCurrentMonth: Boolean = false,
    val selectedType: TransactionType = TransactionType.Income,
    val selectedTab: ReportTab = ReportTab.Mes,
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
) : UiState
