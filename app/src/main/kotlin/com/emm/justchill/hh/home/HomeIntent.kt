package com.emm.justchill.hh.home

import com.emm.domain.shared.Money
import com.emm.justchill.core.mvi.UiIntent

sealed interface HomeIntent : UiIntent {
    data object PreviousMonth : HomeIntent
    data object NextMonth : HomeIntent
    data object JumpToCurrent : HomeIntent
    data class ConfirmRecurring(val templateId: String, val callerAmount: Money?) : HomeIntent
}
