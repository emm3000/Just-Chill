package com.emm.data.backup

import com.emm.data.shared.enumValueOrNull
import com.emm.data.shared.toOccurredAtOrNull
import com.emm.data.shared.toOccurredAtText
import com.emm.domain.loan.LoanPayment
import com.emm.domain.loan.PaymentMethod
import com.emm.domain.shared.LoanId
import com.emm.domain.shared.LoanPaymentId
import com.emm.domain.shared.Money
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class LoanPaymentDto(
    val paymentId: String,
    val loanId: String,
    val amountCents: Long,
    val method: String,
    val paidAt: String,
    val note: String,
)

fun LoanPayment.toDto() = LoanPaymentDto(
    paymentId = id.value,
    loanId = loanId.value,
    amountCents = amount.cents,
    method = method.name,
    paidAt = paidAt.toOccurredAtText(),
    note = note,
)

fun LoanPaymentDto.toEntityOrNull(): LoanPayment? {
    val parsedMethod: PaymentMethod? = enumValueOrNull<PaymentMethod>(method)
    val parsedPaidAt: LocalDateTime? = paidAt.toOccurredAtOrNull()
    return if (parsedMethod == null || parsedPaidAt == null) {
        null
    } else {
        LoanPayment(
            id = LoanPaymentId(paymentId),
            loanId = LoanId(loanId),
            amount = Money(amountCents),
            method = parsedMethod,
            paidAt = parsedPaidAt,
            note = note,
        )
    }
}
