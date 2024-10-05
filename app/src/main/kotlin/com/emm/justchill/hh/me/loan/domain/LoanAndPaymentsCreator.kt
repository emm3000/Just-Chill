package com.emm.justchill.hh.me.loan.domain

import com.emm.justchill.hh.shared.DefaultUniqueIdProvider
import com.emm.justchill.hh.shared.UniqueIdProvider
import com.emm.justchill.hh.me.payment.domain.Payment
import com.emm.justchill.hh.me.payment.domain.PaymentsCreator
import com.emm.justchill.hh.me.payment.domain.PaymentsGenerator
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