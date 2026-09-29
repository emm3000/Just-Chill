package com.emm.justchill.feature.loan

import kotlinx.datetime.LocalDate

fun paymentForm(amountDigits: String = "", remainingCents: Long = 16_000L): LoanPaymentFormUi = LoanPaymentFormUi(
    loanId = "loan-1",
    today = LocalDate(2026, 8, 22),
    remainingCents = remainingCents,
    amountDigits = amountDigits,
)

fun loanSummary(note: String = ""): LoanSummaryUi = LoanSummaryUi(
    personName = "María",
    principal = "S/ 100.00",
    interestPercentLabel = "0%",
    totalDue = "S/ 100.00",
    paidSoFar = "S/ 0.00",
    remaining = "S/ 100.00",
    remainingCents = 10_000L,
    readableLentAt = "1 de agosto de 2026",
    note = note,
)

fun paymentRow(id: String = "pay-1", amount: String = "S/ 300.00", note: String = ""): LoanPaymentRowUi =
    LoanPaymentRowUi(
        paymentId = id,
        amount = amount,
        methodLabel = "Efectivo",
        readablePaidAt = "15 de agosto de 2026",
        note = note,
    )
