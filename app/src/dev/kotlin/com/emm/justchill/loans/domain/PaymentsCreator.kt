package com.emm.justchill.loans.domain

class PaymentsCreator(private val repository: PaymentRepository) {

    suspend fun create(payments: List<Payment>) {
        repository.addAll(payments)
    }
}