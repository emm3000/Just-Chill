package com.emm.justchill.hh.loan

import com.emm.domain.loan.LoanPayment
import com.emm.justchill.hh.shared.SpanishDateFormat
import com.emm.justchill.hh.shared.formatNeutral
import com.emm.justchill.hh.shared.fromCentsToSolesWith

data class LoanPaymentRowUi(
    val paymentId: String,
    val amount: String,
    val methodLabel: String,
    val readablePaidAt: String,
    val note: String,
)

private fun LoanPayment.toUi() = LoanPaymentRowUi(
    paymentId = id.value,
    amount = formatNeutral(fromCentsToSolesWith(amount)),
    methodLabel = method.label,
    readablePaidAt = SpanishDateFormat.longDate(paidAt.date),
    note = note,
)

fun List<LoanPayment>.toUi(): List<LoanPaymentRowUi> = map { it.toUi() }
