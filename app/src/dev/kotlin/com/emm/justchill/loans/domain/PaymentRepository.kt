package com.emm.justchill.loans.domain

interface PaymentRepository {

    suspend fun add(payment: Payment)

    suspend fun addAll(payments: List<Payment>)
}