package com.emm.justchill.core.database.loan

import com.emm.justchill.core.database.Loan_payments
import com.emm.justchill.core.database.shared.enumValueOrNull
import com.emm.justchill.core.database.shared.toOccurredAtOrNull
import com.emm.justchill.core.domain.loan.LoanPayment
import com.emm.justchill.core.domain.loan.PaymentMethod
import com.emm.justchill.core.domain.shared.LoanId
import com.emm.justchill.core.domain.shared.LoanPaymentId
import com.emm.justchill.core.domain.shared.Money

fun Loan_payments.asEntity() = LoanPaymentEntity(
    paymentId = paymentId,
    loanId = loanId,
    amount = amount,
    method = method,
    paidAt = paidAt,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun List<Loan_payments>.asEntity() = map(Loan_payments::asEntity)

fun LoanPaymentEntity.asExternalModelOrNull(): LoanPayment? {
    val parsedMethod = enumValueOrNull<PaymentMethod>(method)
    val parsedPaidAt = paidAt.toOccurredAtOrNull()
    return if (parsedMethod == null || parsedPaidAt == null) {
        null
    } else {
        LoanPayment(
            id = LoanPaymentId(paymentId),
            loanId = LoanId(loanId),
            amount = Money(amount),
            method = parsedMethod,
            paidAt = parsedPaidAt,
            note = note,
        )
    }
}

fun List<LoanPaymentEntity>.asExternalModel() = mapNotNull(LoanPaymentEntity::asExternalModelOrNull)
