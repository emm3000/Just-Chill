package com.emm.justchill.hh.loan

import com.emm.domain.loan.LoanBalance
import com.emm.justchill.hh.shared.SpanishDateFormat
import com.emm.justchill.hh.shared.formatNeutral
import com.emm.justchill.hh.shared.fromCentsToSolesWith

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
    principal = formatNeutral(fromCentsToSolesWith(loan.principal)),
    totalDue = formatNeutral(fromCentsToSolesWith(loan.totalDue)),
    paidSoFar = formatNeutral(fromCentsToSolesWith(paidSoFar)),
    remaining = formatNeutral(fromCentsToSolesWith(remaining)),
    isSettled = remaining.cents == 0L,
    readableLentAt = SpanishDateFormat.longDate(loan.lentAt.date),
)

fun List<LoanBalance>.toUi(): List<LoanRowUi> = map { it.toUi() }
