package com.emm.justchill.hh.me.payment.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.hh.me.payment.domain.Payment
import com.emm.justchill.hh.me.payment.domain.PaymentRepository
import com.emm.justchill.hh.me.payment.domain.PaymentStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PaymentsViewModel(
    loanId: String,
    private val paymentRepository: PaymentRepository,
) : ViewModel() {

    val payments: StateFlow<List<PaymentUi>> = paymentRepository.fetch(loanId)
        .map { it.map(Payment::toUi) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun pay(isPay: Boolean, paymentId: String) = viewModelScope.launch {
        val status: PaymentStatus = if (isPay) PaymentStatus.PAID else PaymentStatus.PENDING
        paymentRepository.pay(status, paymentId)
    }

}