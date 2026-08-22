package com.emm.domain.loan

import com.emm.domain.shared.LoanPaymentId
import com.emm.domain.shared.ensureNotFutureDated
import com.emm.domain.shared.ensurePositiveAmount
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class UpdateLoanPaymentUseCase(
    private val loanRepository: LoanRepository,
    private val loanPaymentRepository: LoanPaymentRepository,
    private val clock: Clock,
    private val zone: TimeZone,
) {

    suspend operator fun invoke(update: LoanPaymentUpdate) {
        ensurePositiveAmount(update.amount)
        ensureNotFutureDated(update.paidAt, clock, zone)
        val loan = loanRepository.byId(update.loanId).first() ?: throw DomainException.NotFound("Loan")
        val existing = requireExistingPayment(update.id)
        val paidSoFar = loanPaymentRepository.paidSoFar(loan.id)
        // The balance excludes the abono being edited: its old amount is subtracted out of
        // paidSoFar before checking the new one, otherwise editing the only abono on a settled
        // loan always fails against its own already-counted amount.
        val remainingBeforeThis = remaining(loan.totalDue, paidSoFar - existing.amount)
        if (update.amount.cents > remainingBeforeThis.cents) {
            throw DomainException.ValidationError(
                "Payment of ${update.amount.cents} exceeds remaining balance of ${remainingBeforeThis.cents}",
                ValidationCode.PaymentExceedsBalance,
            )
        }
        val loanPayment = LoanPayment(
            id = update.id,
            loanId = update.loanId,
            amount = update.amount,
            method = update.method,
            paidAt = update.paidAt,
            note = update.note,
        )
        loanPaymentRepository.update(loanPayment)
    }

    private suspend fun requireExistingPayment(id: LoanPaymentId): LoanPayment =
        loanPaymentRepository.byId(id).first() ?: throw DomainException.NotFound("LoanPayment")
}
