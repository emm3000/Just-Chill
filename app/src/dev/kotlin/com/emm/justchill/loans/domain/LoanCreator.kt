package com.emm.justchill.loans.domain

import com.emm.justchill.hh.domain.transactioncategory.DateAndTimeCombiner

class LoanCreator(
    private val repository: LoanRepository,
    private val dateAndTimeCombiner: DateAndTimeCombiner,
) {

    suspend fun create(loanCreate: LoanCreate) {
        checkNotNull(loanCreate.loanId)

        val dateAndTimeCombined: Long = dateAndTimeCombiner.combine(loanCreate.startDate)
        val amountWithInterest: Double = loanCreate.amountWithInterest

        val loan = Loan(
            loanId = loanCreate.loanId,
            amount = loanCreate.amount.toDouble(),
            amountWithInterest = amountWithInterest,
            interest = loanCreate.interest.toLong(),
            startDate = dateAndTimeCombined,
            duration = loanCreate.durationAtNumber,
            paymentFrequency = loanCreate.frequencyType.name,
            status = PaymentStatus.PENDING.name
        )
        repository.add(loan)
    }
}