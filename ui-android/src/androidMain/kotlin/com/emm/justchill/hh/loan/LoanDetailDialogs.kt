package com.emm.justchill.hh.loan

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.emm.justchill.core.theme.LocalEmmColors

@Composable
fun DeleteLoanDialog(summary: LoanSummaryUi?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
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
            TextButton(onClick = onConfirm) {
                Text(text = "Borrar", color = colors.danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
fun DeleteLoanPaymentDialog(payment: LoanPaymentRowUi?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
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
            TextButton(onClick = onConfirm) {
                Text(text = "Borrar", color = colors.danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
