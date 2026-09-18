package com.emm.justchill.feature.loan

import com.emm.justchill.core.ui.loan.PersonBalanceUi
import com.emm.justchill.core.ui.mvi.UiState

data class LoansUiState(val people: List<PersonBalanceUi> = emptyList()) : UiState
