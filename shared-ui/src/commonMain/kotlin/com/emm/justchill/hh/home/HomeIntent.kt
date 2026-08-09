package com.emm.justchill.hh.home

import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.justchill.core.mvi.UiIntent

sealed interface HomeIntent : UiIntent {
    data object PreviousMonth : HomeIntent
    data object NextMonth : HomeIntent

    /**
     * [period] travels with the intent rather than being read from the selected month: a pending
     * item can be a month the user is not looking at, which is the whole point of catching up.
     */
    data class ConfirmRecurring(val templateId: String, val period: YearMonth, val callerAmount: Money?) : HomeIntent

    /** Settles [period] with no transaction — the month the user genuinely did not pay. */
    data class SkipRecurring(val templateId: String, val period: YearMonth) : HomeIntent
}
