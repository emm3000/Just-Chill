package com.emm.justchill.hh.loan

import com.emm.domain.loan.Loan
import com.emm.domain.loan.remaining
import com.emm.domain.shared.Money
import com.emm.justchill.hh.shared.SpanishDateFormat
import com.emm.justchill.hh.shared.formatNeutral
import com.emm.justchill.hh.shared.fromCentsToSolesWith

data class LoanSummaryUi(
    val personName: String,
    val principal: String,
    val interestPercentLabel: String,
    val totalDue: String,
    val paidSoFar: String,
    val remaining: String,
    val isSettled: Boolean,
    val readableLentAt: String,
    val note: String,
)

fun loanSummaryUi(loan: Loan, paidSoFar: Money): LoanSummaryUi {
    val remainingMoney = remaining(loan.totalDue, paidSoFar)
    return LoanSummaryUi(
        personName = loan.personName,
        principal = formatNeutral(fromCentsToSolesWith(loan.principal)),
        interestPercentLabel = "${bpsToPercentText(loan.interestBps)}%",
        totalDue = formatNeutral(fromCentsToSolesWith(loan.totalDue)),
        paidSoFar = formatNeutral(fromCentsToSolesWith(paidSoFar)),
        remaining = formatNeutral(fromCentsToSolesWith(remainingMoney)),
        isSettled = remainingMoney.cents == 0L,
        readableLentAt = SpanishDateFormat.longDate(loan.lentAt.date),
        note = loan.note,
    )
}
