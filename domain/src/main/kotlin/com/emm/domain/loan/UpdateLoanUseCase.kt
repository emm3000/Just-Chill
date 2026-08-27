package com.emm.domain.loan

import com.emm.domain.shared.LoanId
import com.emm.domain.shared.ensureNotFutureDated
import com.emm.domain.shared.ensurePositiveAmount
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class UpdateLoanUseCase(
    private val loanRepository: LoanRepository,
    private val loanPaymentRepository: LoanPaymentRepository,
    private val clock: Clock,
    private val zone: TimeZone,
) {

    suspend operator fun invoke(loanId: LoanId, loanUpdate: LoanUpdate) {
        ensurePersonProvided(loanUpdate.personName)
        ensurePositiveAmount(loanUpdate.principal)
        ensureInterestInRange(loanUpdate.interestBps)
        ensureNotFutureDated(loanUpdate.lentAt, clock, zone)
        loanRepository.byId(loanId).first() ?: throw DomainException.NotFound("Loan")
        val newTotalDue = totalDue(loanUpdate.principal, loanUpdate.interestBps)
        val paidSoFar = loanPaymentRepository.paidSoFar(loanId)
        if (newTotalDue.cents < paidSoFar.cents) {
            throw DomainException.ValidationError(
                "New total is below what has already been paid",
                ValidationCode.TotalBelowPaid,
            )
        }
        val personName = loanUpdate.personName.trim()
        val loan = Loan(
            id = loanId,
            personName = personName,
            personKey = personKey(personName),
            principal = loanUpdate.principal,
            interestBps = loanUpdate.interestBps,
            totalDue = newTotalDue,
            note = loanUpdate.note,
            lentAt = loanUpdate.lentAt,
        )
        loanRepository.update(loan)
    }
}
