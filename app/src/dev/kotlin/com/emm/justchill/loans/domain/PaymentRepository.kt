package com.emm.justchill.loans.domain

import kotlinx.coroutines.flow.Flow

interface PaymentRepository {

    suspend fun add(payment: Payment)

    suspend fun addAll(payments: List<Payment>)

    fun fetch(loanId: String): Flow<List<Payment>>

    fun all(): Flow<List<Payment>>

    suspend fun pay(paymentStatus: PaymentStatus, paymentId: String)
}