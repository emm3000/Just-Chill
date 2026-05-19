package com.emm.justchill.hh.report

import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.UiIntent

sealed interface ReportIntent : UiIntent {
    data object PreviousMonth : ReportIntent
    data object NextMonth : ReportIntent
    data object JumpToCurrent : ReportIntent
    data class SelectType(val type: TransactionType) : ReportIntent
}
