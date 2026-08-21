package com.emm.domain.loan

import com.emm.domain.shared.LoanId
import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.shared.ensureNotFutureDated
import com.emm.domain.shared.ensurePositiveAmount
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class CreateLoanUseCase(
    private val loanRepository: LoanRepository,
    private val uniqueIdProvider: UniqueIdProvider,
    private val clock: Clock,
    private val zone: TimeZone,
) {

    suspend operator fun invoke(loanInsert: LoanInsert) {
        ensurePersonProvided(loanInsert.personName)
        ensurePositiveAmount(loanInsert.principal)
        ensureInterestInRange(loanInsert.interestBps)
        ensureNotFutureDated(loanInsert.lentAt, clock, zone)
        val loan = Loan(
            id = LoanId(uniqueIdProvider.id),
            personName = loanInsert.personName.trim(),
            personKey = personKey(loanInsert.personName),
            principal = loanInsert.principal,
            interestBps = loanInsert.interestBps,
            totalDue = totalDue(loanInsert.principal, loanInsert.interestBps),
            note = loanInsert.note,
            lentAt = loanInsert.lentAt,
        )
        loanRepository.create(loan)
    }
}
