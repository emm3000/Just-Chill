package com.emm.justchill.loans.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PaymentsCreator(private val repository: PaymentRepository) {

    suspend fun create(paymentModels: List<Payment>) = withContext(Dispatchers.IO) {
        repository.addAll(paymentModels)
    }
}