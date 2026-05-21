package com.emm.justchill.hh.report

import androidx.compose.runtime.Stable
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiState

@Stable
data class ReportUiState(
    val month: YearMonth = YearMonth.current(),
    val selectedType: TransactionType = TransactionType.Income,
    val selectedTab: ReportTab = ReportTab.Mes,
    val totalFormatted: String = "S/ 0.00",
    val comparisonText: String? = null,
    val comparisonIsPositive: Boolean? = null,
    val comparisonAmountFormatted: String? = null,
    val comparisonPercent: Int = 0,
    val shares: List<CategoryShare> = emptyList(),
    val isEmpty: Boolean = false,
    val movementCount: Int = 0,
    val averageFormatted: String = "S/ 0",
    val trends: TrendsUiData = TrendsUiData(),
) : UiState
