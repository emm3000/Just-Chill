package com.emm.justchill.hh.seetransactions

import androidx.compose.runtime.Stable
import com.emm.justchill.core.mvi.UiState

@Stable
data class SeeTransactionsUiState(
    val days: List<DayGroup> = emptyList(),
) : UiState
