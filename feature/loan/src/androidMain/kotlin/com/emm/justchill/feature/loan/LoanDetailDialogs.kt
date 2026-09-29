package com.emm.justchill.feature.loan

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.ui.atoms.EmmDialog
import com.emm.justchill.core.ui.atoms.IconBtnTone
import com.emm.justchill.core.ui.atoms.inFlightDialogProperties
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
internal fun DeleteLoanDialog(
    summary: LoanSummaryUi,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current

    EmmDialog(
        title = "¿Borrar este préstamo?",
        confirmLabel = "Borrar",
        onConfirm = onConfirm,
        dismissLabel = "Cancelar",
        onDismiss = onDismiss,
        properties = inFlightDialogProperties(isDeleting),
        actionsEnabled = !isDeleting,
        confirmTone = IconBtnTone.Danger,
    ) {
        Text(
            text = "Prestado el ${summary.readableLentAt} por ${summary.principal}. " +
                "Se borra junto con sus abonos registrados.",
            style = type.bodyM,
            color = colors.textSecondary,
        )
    }
}

private val previewLoanSummary = LoanSummaryUi(
    personName = "Juan",
    principal = "S/ 1,200.00",
    interestPercentLabel = "5%",
    totalDue = "S/ 1,260.00",
    paidSoFar = "S/ 300.00",
    remaining = "S/ 960.00",
    remainingCents = 96_000L,
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

@Preview
@Composable
private fun DeleteLoanDialogDeletingPreview() {
    EmmTheme {
        DeleteLoanDialog(
            summary = previewLoanSummary,
            isDeleting = true,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Composable
internal fun DeleteLoanPaymentDialog(
    payment: LoanPaymentRowUi,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current

    EmmDialog(
        title = "¿Borrar este abono?",
        confirmLabel = "Borrar",
        onConfirm = onConfirm,
        dismissLabel = "Cancelar",
        onDismiss = onDismiss,
        properties = inFlightDialogProperties(isDeleting),
        actionsEnabled = !isDeleting,
        confirmTone = IconBtnTone.Danger,
    ) {
        Text(
            text = "Abono de ${payment.amount} del ${payment.readablePaidAt}. " +
                "El préstamo recupera ese monto como pendiente.",
            style = type.bodyM,
            color = colors.textSecondary,
        )
    }
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

@Preview
@Composable
private fun DeleteLoanPaymentDialogDeletingPreview() {
    EmmTheme {
        DeleteLoanPaymentDialog(
            payment = previewLoanPayment,
            isDeleting = true,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
