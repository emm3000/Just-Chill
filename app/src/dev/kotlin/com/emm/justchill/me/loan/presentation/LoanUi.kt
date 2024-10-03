package com.emm.justchill.me.loan.presentation

import com.emm.justchill.hh.domain.shared.fromCentsToSolesWith
import com.emm.justchill.hh.presentation.transaction.DateUtils
import com.emm.justchill.me.loan.domain.Loan

data class LoanUi(
    val loanId: String,
    val amount: String,
    val amountWithInterest: String,
    val interest: Long,
    val startDate: Long,
    val duration: Long,
    val status: String,
    val driverId: Long,
    val readableDate: String,
    val readableTime: String,
)

fun Loan.toUi(): LoanUi {
    val formattedAmount: String = fromCentsToSolesWith(amount)
    val formattedAmountWithInterest: String = fromCentsToSolesWith(amountWithInterest)
    return LoanUi(
        loanId = loanId,
        amount = "S/ $formattedAmount",
        amountWithInterest = "S/ $formattedAmountWithInterest",
        interest = interest,
        startDate = startDate,
        duration = duration,
        status = status,
        driverId = driverId,
        readableDate = DateUtils.millisToReadableFormat(startDate),
        readableTime = DateUtils.readableTime(startDate)
    )
}