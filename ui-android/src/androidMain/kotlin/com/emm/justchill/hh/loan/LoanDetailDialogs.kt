package com.emm.justchill.hh.loan

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors

@Composable
internal fun DeleteLoanDialog(
    summary: LoanSummaryUi?,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalEmmColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Borrar este préstamo?") },
        text = {
            Text(
                "Prestado el ${summary?.readableLentAt.orEmpty()} por ${summary?.principal.orEmpty()}. " +
                    "Se borra junto con sus abonos registrados.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isDeleting) {
                Text(text = "Borrar", color = colors.danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

private val previewLoanSummary = LoanSummaryUi(
    personName = "Juan",
    principal = "S/ 1,200.00",
    interestPercentLabel = "5%",
    totalDue = "S/ 1,260.00",
    paidSoFar = "S/ 300.00",
    remaining = "S/ 960.00",
    isSettled = false,
    readableLentAt = "12 de agosto de 2026",
    note = "Para el arreglo del carro",
)

@Preview
@Composable
private fun DeleteLoanDialogPreview() {
    EmmTheme {
        DeleteLoanDialog(
            summary = previewLoanSummary,
            isDeleting = false,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Composable
internal fun DeleteLoanPaymentDialog(
    payment: LoanPaymentRowUi?,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalEmmColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Borrar este abono?") },
        text = {
            Text(
                "Abono de ${payment?.amount.orEmpty()} del ${payment?.readablePaidAt.orEmpty()}. " +
                    "El préstamo recupera ese monto como pendiente.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isDeleting) {
                Text(text = "Borrar", color = colors.danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

private val previewLoanPayment = LoanPaymentRowUi(
    paymentId = "1",
    amount = "S/ 300.00",
    methodLabel = "Yape",
    readablePaidAt = "15 de agosto de 2026",
    note = "Primer abono",
)

@Preview
@Composable
private fun DeleteLoanPaymentDialogPreview() {
    EmmTheme {
        DeleteLoanPaymentDialog(
            payment = previewLoanPayment,
            isDeleting = false,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
