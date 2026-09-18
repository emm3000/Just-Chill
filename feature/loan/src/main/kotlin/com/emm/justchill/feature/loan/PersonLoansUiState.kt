package com.emm.justchill.feature.loan

import com.emm.justchill.core.ui.mvi.UiState

data class PersonLoansUiState(val personName: String = "", val loans: List<LoanRowUi> = emptyList()) : UiState
