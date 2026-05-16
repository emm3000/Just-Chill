package com.emm.justchill.hh.seetransactions

import com.emm.justchill.core.mvi.UiState

data class SeeTransactionsUiState(
    val days: List<DayGroup> = emptyList(),
) : UiState
