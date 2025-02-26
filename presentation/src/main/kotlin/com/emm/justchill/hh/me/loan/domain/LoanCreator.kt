package com.emm.justchill.hh.me.loan.domain

import com.emm.justchill.hh.shared.DateAndTimeCombiner
import com.emm.justchill.hh.me.payment.domain.PaymentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoanCreator(
    private val repository: LoanRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
) {

    suspend fun create(loanCreate: LoanCreate) = withContext(Dispatchers.IO) {

        val dateAndTimeCombined: Long = dateAndTimeCombiner.combineWithUtc(loanCreate.startDate)
        val amountWithInterest: Double = loanCreate.amountWithInterest

        val loan: Loan = buildLoan(
            loanCreate = loanCreate,
            amountWithInterest = amountWithInterest,
            dateAndTimeCombined = dateAndTimeCombined
        )

        repository.add(loan)
    }

    private fun buildLoan(
        loanCreate: LoanCreate,
        amountWithInterest: Double,
        dateAndTimeCombined: Long,
    ): Loan {
        checkNotNull(loanCreate.loanId)

        val loanId = loanCreate.loanId

        return Loan(
            loanId = loanId,
            amount = loanCreate.amount.toDouble(),
            amountWithInterest = amountWithInterest,
            interest = loanCreate.interest.toLong(),
            startDate = dateAndTimeCombined,
            duration = loanCreate.durationAtNumber,
            status = PaymentStatus.PENDING.name,
            driverId = loanCreate.driverId,
        )
    }
}