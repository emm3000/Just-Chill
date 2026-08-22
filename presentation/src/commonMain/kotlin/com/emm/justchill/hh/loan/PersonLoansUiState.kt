package com.emm.justchill.hh.loan

import com.emm.justchill.core.mvi.UiState

data class PersonLoansUiState(
    val personName: String = "",
    val loans: List<LoanRowUi> = emptyList(),
    val pendingDelete: String? = null,
) : UiState
