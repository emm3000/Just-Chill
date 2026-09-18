package com.emm.justchill.hh.report

import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.mvi.UiIntent

sealed interface ReportIntent : UiIntent {
    data object PreviousMonth : ReportIntent
    data object NextMonth : ReportIntent
    data object JumpToCurrent : ReportIntent
    data class SelectType(val type: TransactionType) : ReportIntent
    data class SelectTab(val tab: ReportTab) : ReportIntent
    data class SelectMonth(val month: YearMonth) : ReportIntent
    data object ShareReport : ReportIntent

    data object OnMonthSheetRequested : ReportIntent
    data object OnMonthSheetDismissed : ReportIntent
}
