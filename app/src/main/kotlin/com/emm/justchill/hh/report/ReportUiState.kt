package com.emm.justchill.hh.report

import androidx.compose.runtime.Stable
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiState

@Stable
data class ReportUiState(
    val month: YearMonth = YearMonth.current(),
    val selectedType: TransactionType = TransactionType.Income,
    val totalFormatted: String = "S/ 0",
    val comparisonText: String? = null,
    val comparisonIsPositive: Boolean? = null,
    val shares: List<CategoryShare> = emptyList(),
    val isEmpty: Boolean = false,
) : UiState
