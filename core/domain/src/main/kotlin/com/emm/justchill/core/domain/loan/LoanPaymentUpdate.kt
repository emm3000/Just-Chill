package com.emm.justchill.core.domain.loan

import com.emm.justchill.core.domain.shared.LoanPaymentId
import com.emm.justchill.core.domain.shared.Money
import kotlinx.datetime.LocalDateTime

data class LoanPaymentUpdate(
    val id: LoanPaymentId,
    val amount: Money,
    val method: PaymentMethod,
    val paidAt: LocalDateTime,
    val note: String,
)
