package com.emm.domain.loan

import com.emm.domain.shared.LoanId
import com.emm.domain.shared.LoanPaymentId
import com.emm.domain.shared.Money
import kotlinx.datetime.LocalDateTime

data class LoanPaymentUpdate(
    val id: LoanPaymentId,
    val loanId: LoanId,
    val amount: Money,
    val method: PaymentMethod,
    val paidAt: LocalDateTime,
    val note: String,
)
