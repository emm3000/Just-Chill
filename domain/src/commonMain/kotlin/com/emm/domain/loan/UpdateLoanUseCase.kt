package com.emm.domain.loan

import com.emm.domain.shared.LoanId
import com.emm.domain.shared.ensureNotFutureDated
import com.emm.domain.shared.ensurePositiveAmount
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class UpdateLoanUseCase(
    private val loanRepository: LoanRepository,
    private val clock: Clock,
    private val zone: TimeZone,
) {

    suspend operator fun invoke(loanId: LoanId, loanUpdate: LoanUpdate) {
        ensurePersonProvided(loanUpdate.personName)
        ensurePositiveAmount(loanUpdate.principal)
        ensureInterestInRange(loanUpdate.interestBps)
        ensureNotFutureDated(loanUpdate.lentAt, clock, zone)
        val loan = Loan(
            id = loanId,
            personName = loanUpdate.personName.trim(),
            personKey = personKey(loanUpdate.personName),
            principal = loanUpdate.principal,
            interestBps = loanUpdate.interestBps,
            totalDue = totalDue(loanUpdate.principal, loanUpdate.interestBps),
            note = loanUpdate.note,
            lentAt = loanUpdate.lentAt,
        )
        loanRepository.update(loan)
    }
}
