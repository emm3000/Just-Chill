package com.emm.justchill.feature.loan

import com.emm.justchill.core.presentation.mvi.UiState

data class LoansUiState(val people: List<PersonRowUi> = emptyList()) : UiState
