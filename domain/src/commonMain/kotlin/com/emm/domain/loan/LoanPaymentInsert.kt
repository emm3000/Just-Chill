package com.emm.domain.loan

import com.emm.domain.shared.LoanId
import com.emm.domain.shared.Money
import kotlinx.datetime.LocalDateTime

data class LoanPaymentInsert(
    val loanId: LoanId,
    val amount: Money,
    val method: PaymentMethod,
    val paidAt: LocalDateTime,
    val note: String,
)
