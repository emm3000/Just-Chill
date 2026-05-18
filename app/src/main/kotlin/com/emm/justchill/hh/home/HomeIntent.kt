package com.emm.justchill.hh.home

import com.emm.justchill.core.mvi.UiIntent

sealed interface HomeIntent : UiIntent {
    data object PreviousMonth : HomeIntent
    data object NextMonth : HomeIntent
    data object JumpToToday : HomeIntent
}
