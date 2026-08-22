package com.emm.justchill.hh.loan

import com.emm.justchill.core.mvi.UiState

data class LoansUiState(val people: List<PersonBalanceUi> = emptyList()) : UiState
