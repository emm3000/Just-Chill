package com.emm.justchill.hh.loan

import com.emm.justchill.core.mvi.UiState

data class LoanDetailUiState(
    val summary: LoanSummaryUi? = null,
    val payments: List<LoanPaymentRowUi> = emptyList(),
    val pendingDeleteLoan: Boolean = false,
    val isDeletingLoan: Boolean = false,
    val pendingDeletePaymentId: String? = null,
    val isDeletingPayment: Boolean = false,
    val payment: LoanPaymentFormUi? = null,
) : UiState
