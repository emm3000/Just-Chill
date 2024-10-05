package com.emm.justchill.me.loan.domain

import com.emm.justchill.hh.data.shared.DefaultUniqueIdProvider
import com.emm.justchill.hh.domain.shared.UniqueIdProvider
import com.emm.justchill.me.payment.domain.Payment
import com.emm.justchill.me.payment.domain.PaymentsCreator
import com.emm.justchill.me.payment.domain.PaymentsGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoanAndPaymentsCreator(
    private val loanCreator: LoanCreator,
    private val paymentsCreator: PaymentsCreator,
    private val paymentsGenerator: PaymentsGenerator,
    private val uniqueIdProvider: UniqueIdProvider = DefaultUniqueIdProvider,
) {

    suspend fun create(loanCreate: LoanCreate) = withContext(Dispatchers.IO) {

        val loanId: String = uniqueIdProvider.uniqueId
        val loanCreateWithId = loanCreate.copy(loanId = loanId)

        val payments: List<Payment> = paymentsGenerator.generate(
            loanCreate = loanCreateWithId
        )

        loanCreator.create(loanCreateWithId)
        paymentsCreator.create(payments)
    }
}