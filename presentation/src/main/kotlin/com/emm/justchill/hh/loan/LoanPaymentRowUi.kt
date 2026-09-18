package com.emm.justchill.hh.loan

import com.emm.justchill.core.domain.loan.LoanPayment
import com.emm.justchill.core.ui.format.SpanishDateFormat
import com.emm.justchill.core.ui.format.formatNeutral
import com.emm.justchill.core.ui.format.fromCentsToSolesWith

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
