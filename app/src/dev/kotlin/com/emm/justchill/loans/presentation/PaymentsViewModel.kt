package com.emm.justchill.loans.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.loans.domain.Payment
import com.emm.justchill.loans.domain.PaymentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PaymentsViewModel(
    loanId: String,
    paymentRepository: PaymentRepository,
) : ViewModel() {

    val payments: StateFlow<List<PaymentUi>> = paymentRepository.fetch(loanId)
        .map { it.map(Payment::toUi) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

}