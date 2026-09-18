package com.emm.justchill.hh.loan

import com.emm.justchill.core.ui.mvi.UiState

data class LoansUiState(val people: List<PersonBalanceUi> = emptyList()) : UiState
