package com.emm.justchill.loans.domain

import com.emm.justchill.hh.domain.shared.DateAndTimeCombiner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoanCreator(
    private val repository: LoanRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
) {

    suspend fun create(loanCreate: LoanCreate) = withContext(Dispatchers.IO) {

        val dateAndTimeCombined: Long = dateAndTimeCombiner.combine(loanCreate.startDate)
        val amountWithInterest: Double = loanCreate.amountWithInterest

        val loan = buildLoan(
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
            paymentFrequency = loanCreate.frequencyType.name,
            status = PaymentStatus.PENDING.name
        )
    }
}