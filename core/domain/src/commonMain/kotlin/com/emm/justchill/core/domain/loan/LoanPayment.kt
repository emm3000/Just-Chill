package com.emm.justchill.core.domain.loan

import com.emm.justchill.core.domain.shared.LoanId
import com.emm.justchill.core.domain.shared.LoanPaymentId
import com.emm.justchill.core.domain.shared.Money
import kotlinx.datetime.LocalDateTime

data class LoanPayment(
    val id: LoanPaymentId,
    val loanId: LoanId,
    val amount: Money,
    val method: PaymentMethod,
    val paidAt: LocalDateTime,
    val note: String,
)
