package com.emm.justchill.core.domain.loan

import com.emm.justchill.core.domain.shared.LoanId
import com.emm.justchill.core.domain.shared.Money
import kotlinx.datetime.LocalDateTime

data class LoanPaymentInsert(
    val loanId: LoanId,
    val amount: Money,
    val method: PaymentMethod,
    val paidAt: LocalDateTime,
    val note: String,
)
