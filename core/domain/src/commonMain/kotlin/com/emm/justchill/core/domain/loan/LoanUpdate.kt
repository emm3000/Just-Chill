package com.emm.justchill.core.domain.loan

import com.emm.justchill.core.domain.shared.Money
import kotlinx.datetime.LocalDateTime

data class LoanUpdate(
    val personName: String,
    val principal: Money,
    val interestBps: Int,
    val note: String,
    val lentAt: LocalDateTime,
)
