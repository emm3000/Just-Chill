package com.emm.domain.loan

import com.emm.domain.shared.Money

data class LoanBalance(val loan: Loan, val paidSoFar: Money, val remaining: Money)
