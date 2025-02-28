package com.emm.justchill.me.payment.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.justchill.EmmDatabase
import com.emm.justchill.Payments
import com.emm.justchill.PaymentsQueries
import com.emm.justchill.me.payment.domain.Payment
import com.emm.justchill.me.payment.domain.PaymentRepository
import com.emm.justchill.me.payment.domain.PaymentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LocalPaymentRepository(
    private val emmDatabase: EmmDatabase,
) : PaymentRepository {

    private val pq: PaymentsQueries
        get() = emmDatabase.paymentsQueries

    override suspend fun add(payment: Payment) = withContext(Dispatchers.IO) {
        pq.insert(
            paymentId = payment.paymentId,
            loanId = payment.loanId,
            dueDate = payment.dueDate,
            amount = payment.amount,
            status = payment.status.name,
        )
    }

    override suspend fun addAll(payments: List<Payment>) = withContext(Dispatchers.IO) {
        pq.transaction {
            payments.forEach {
                pq.insert(
                    paymentId = it.paymentId,
                    loanId = it.loanId,
                    dueDate = it.dueDate,
                    amount = it.amount,
                    status = it.status.name,
                )
            }
        }
    }

    override fun fetch(loanId: String): Flow<List<Payment>> {
        return pq.fetch(loanId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map(::toDomain)
    }

    override fun all(): Flow<List<Payment>> {
        return pq.all()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map(::toDomain)
    }

    override suspend fun pay(
        paymentStatus: PaymentStatus,
        paymentId: String,
    ) = withContext(Dispatchers.IO) {
        pq.pay(paymentStatus.name, paymentId)
    }

    private fun toDomain(payments: List<Payments>): List<Payment> {
        return payments.map {
            Payment(
                paymentId = it.paymentId,
                loanId = it.loanId,
                dueDate = it.dueDate,
                amount = it.amount,
                status = try {
                    PaymentStatus.valueOf(it.status)
                } catch (e: Throwable) {
                    PaymentStatus.PENDING
                },
            )
        }
    }
}