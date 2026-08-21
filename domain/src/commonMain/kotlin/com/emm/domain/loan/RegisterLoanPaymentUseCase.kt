package com.emm.domain.loan

import com.emm.domain.shared.LoanPaymentId
import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.shared.ensureNotFutureDated
import com.emm.domain.shared.ensurePositiveAmount
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class RegisterLoanPaymentUseCase(
    private val loanRepository: LoanRepository,
    private val loanPaymentRepository: LoanPaymentRepository,
    private val uniqueIdProvider: UniqueIdProvider,
    private val clock: Clock,
    private val zone: TimeZone,
) {

    suspend operator fun invoke(insert: LoanPaymentInsert) {
        ensurePositiveAmount(insert.amount)
        ensureNotFutureDated(insert.paidAt, clock, zone)
        val loan = loanRepository.byId(insert.loanId).first() ?: throw DomainException.NotFound("Loan")
        val paidSoFar = loanPaymentRepository.paidSoFar(loan.id)
        val remaining = remaining(loan.totalDue, paidSoFar)
        if (insert.amount.cents > remaining.cents) {
            throw DomainException.ValidationError(
                "Payment of ${insert.amount.cents} exceeds remaining balance of ${remaining.cents}",
                ValidationCode.PaymentExceedsBalance,
            )
        }
        val loanPayment = LoanPayment(
            id = LoanPaymentId(uniqueIdProvider.id),
            loanId = insert.loanId,
            amount = insert.amount,
            method = insert.method,
            paidAt = insert.paidAt,
            note = insert.note,
        )
        loanPaymentRepository.create(loanPayment)
    }
}
