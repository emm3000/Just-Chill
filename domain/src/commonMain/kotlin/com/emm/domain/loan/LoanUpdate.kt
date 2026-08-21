package com.emm.domain.loan

import com.emm.domain.shared.Money
import kotlinx.datetime.LocalDateTime

data class LoanUpdate(
    val personName: String,
    val principal: Money,
    val interestBps: Int,
    val note: String,
    val lentAt: LocalDateTime,
)
