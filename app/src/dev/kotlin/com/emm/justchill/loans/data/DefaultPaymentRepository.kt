package com.emm.justchill.loans.data

import android.util.Log
import com.emm.justchill.loans.domain.Payment
import com.emm.justchill.loans.domain.PaymentRepository

class DefaultPaymentRepository : PaymentRepository {

    override suspend fun add(payment: Payment) {
        Log.e("aea", payment.toString())
    }

    override suspend fun addAll(payments: List<Payment>) {
        payments.forEach {
            Log.e("aea", it.amount.toString())
        }
    }
}