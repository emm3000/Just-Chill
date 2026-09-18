package com.emm.justchill.feature.loan

import com.emm.justchill.core.domain.loan.LoanBalance
import com.emm.justchill.core.ui.format.SpanishDateFormat
import com.emm.justchill.core.ui.format.format
import com.emm.justchill.core.ui.format.formatNeutral

data class LoanRowUi(
    val loanId: String,
    val principal: String,
    val totalDue: String,
    val paidSoFar: String,
    val remaining: String,
    val isSettled: Boolean,
    val readableLentAt: String,
)

private fun LoanBalance.toUi() = LoanRowUi(
    loanId = loan.id.value,
    principal = formatNeutral(loan.principal.format()),
    totalDue = formatNeutral(loan.totalDue.format()),
    paidSoFar = formatNeutral(paidSoFar.format()),
    remaining = formatNeutral(remaining.format()),
    isSettled = remaining.cents == 0L,
    readableLentAt = SpanishDateFormat.longDate(loan.lentAt.date),
)

fun List<LoanBalance>.toUi(): List<LoanRowUi> = map { it.toUi() }
