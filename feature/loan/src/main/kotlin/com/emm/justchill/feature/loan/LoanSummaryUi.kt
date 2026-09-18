package com.emm.justchill.feature.loan

import com.emm.justchill.core.domain.loan.Loan
import com.emm.justchill.core.domain.loan.remaining
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.ui.format.SpanishDateFormat
import com.emm.justchill.core.ui.format.format
import com.emm.justchill.core.ui.format.formatNeutral

data class LoanSummaryUi(
    val personName: String,
    val principal: String,
    val interestPercentLabel: String,
    val totalDue: String,
    val paidSoFar: String,
    val remaining: String,
    val remainingCents: Long,
    val readableLentAt: String,
    val note: String,
) {
    val isSettled: Boolean get() = remainingCents == 0L
}

fun loanSummaryUi(loan: Loan, paidSoFar: Money): LoanSummaryUi {
    val remainingMoney = remaining(loan.totalDue, paidSoFar)
    return LoanSummaryUi(
        personName = loan.personName,
        principal = formatNeutral(loan.principal.format()),
        interestPercentLabel = "${bpsToPercentText(loan.interestBps)}%",
        totalDue = formatNeutral(loan.totalDue.format()),
        paidSoFar = formatNeutral(paidSoFar.format()),
        remaining = formatNeutral(remainingMoney.format()),
        remainingCents = remainingMoney.cents,
        readableLentAt = SpanishDateFormat.longDate(loan.lentAt.date),
        note = loan.note,
    )
}
