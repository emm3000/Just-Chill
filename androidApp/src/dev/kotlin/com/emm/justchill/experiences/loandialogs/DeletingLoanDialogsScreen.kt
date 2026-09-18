package com.emm.justchill.experiences.loandialogs

import androidx.compose.runtime.Composable
import com.emm.justchill.feature.loan.LoanDetailScreen
import com.emm.justchill.feature.loan.LoanDetailUiState
import com.emm.justchill.feature.loan.LoanPaymentRowUi
import com.emm.justchill.feature.loan.LoanSummaryUi

private val summary: LoanSummaryUi = LoanSummaryUi(
    personName = "Juan",
    principal = "S/ 50.00",
    interestPercentLabel = "0%",
    totalDue = "S/ 50.00",
    paidSoFar = "S/ 1.00",
    remaining = "S/ 49.00",
    remainingCents = 4_900L,
    readableLentAt = "17 de septiembre de 2026",
    note = "",
)

private val payment: LoanPaymentRowUi = LoanPaymentRowUi(
    paymentId = "1",
    amount = "S/ 1.00",
    methodLabel = "Efectivo",
    readablePaidAt = "18 de septiembre de 2026",
    note = "",
)

@Composable
fun DeletingLoanDialogScreen() {
    LoanDetailScreen(
        state = LoanDetailUiState(
            summary = summary,
            payments = listOf(payment),
            pendingDeleteLoan = true,
            isDeletingLoan = true,
        ),
        onIntent = {},
        onBack = {},
    )
}

@Composable
fun DeletingLoanPaymentDialogScreen() {
    LoanDetailScreen(
        state = LoanDetailUiState(
            summary = summary,
            payments = listOf(payment),
            pendingDeletePaymentId = payment.paymentId,
            isDeletingPayment = true,
        ),
        onIntent = {},
        onBack = {},
    )
}
