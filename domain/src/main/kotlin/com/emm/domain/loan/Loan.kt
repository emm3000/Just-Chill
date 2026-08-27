package com.emm.domain.loan

import com.emm.domain.shared.LoanId
import com.emm.domain.shared.Money
import kotlinx.datetime.LocalDateTime

data class Loan(
    val id: LoanId,
    val personName: String,
    val personKey: String,
    val principal: Money,
    val interestBps: Int,
    val totalDue: Money,
    val note: String,
    val lentAt: LocalDateTime,
)
