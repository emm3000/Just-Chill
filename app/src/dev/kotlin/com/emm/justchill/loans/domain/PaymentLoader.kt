package com.emm.justchill.loans.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PaymentLoader(private val repository: PaymentRepository) {

    suspend fun load(): List<Payment> = withContext(Dispatchers.IO) {
        return@withContext repository.fetch()
    }
}