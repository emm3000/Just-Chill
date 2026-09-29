package com.emm.justchill.core.domain.loan

import com.emm.justchill.core.domain.shared.Money

data class LoanBalance(val loan: Loan, val paidSoFar: Money) {
    val remaining: Money get() = remaining(loan.totalDue, paidSoFar)
}
