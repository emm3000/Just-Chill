package com.emm.justchill.hh.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.CtaInteraction
import com.emm.justchill.core.ui.atoms.CtaTone
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.IconBtnTone
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.atoms.StickyCTA

@Composable
fun LoanDetailScreen(
    state: LoanDetailUiState,
    onIntent: (LoanDetailIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val summary = state.summary

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        JcTopBar(
            title = summary?.personName.orEmpty(),
            left = {
                IconBtn(icon = Icons.AutoMirrored.Outlined.ArrowBack, onClick = onBack, contentDescription = "Volver")
            },
            right = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconBtn(
                        icon = Icons.Outlined.Edit,
                        onClick = { onIntent(LoanDetailIntent.OnEditLoanClick) },
                        contentDescription = "Editar préstamo",
                    )
                    IconBtn(
                        icon = Icons.Outlined.Delete,
                        tone = IconBtnTone.Danger,
                        onClick = { onIntent(LoanDetailIntent.OnDeleteLoanClick) },
                        contentDescription = "Eliminar préstamo",
                    )
                }
            },
        )
        Hairline()

        if (summary != null) {
            Column(modifier = Modifier.weight(1f)) {
                LoanSummaryCard(summary = summary, modifier = Modifier.fillMaxWidth().padding(16.dp))
                Eyebrow(text = "ABONOS", modifier = Modifier.padding(horizontal = 16.dp))
                LoanPaymentsList(
                    payments = state.payments,
                    onDeleteClick = { paymentId -> onIntent(LoanDetailIntent.OnDeletePaymentClick(paymentId)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 8.dp),
                )
            }

            StickyCTA(
                label = "Registrar abono",
                tone = CtaTone.Accent,
                interaction = if (summary.isSettled) CtaInteraction.Disabled else CtaInteraction.Enabled,
                onClick = { onIntent(LoanDetailIntent.PaymentFormIntent.OnAddPaymentClick) },
            )
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.textTertiary)
            }
        }
    }

    if (state.pendingDeleteLoan) {
        DeleteLoanDialog(
            summary = summary,
            isDeleting = state.isDeletingLoan,
            onConfirm = { onIntent(LoanDetailIntent.OnDeleteLoanConfirm) },
            onDismiss = { onIntent(LoanDetailIntent.OnDeleteLoanDismiss) },
        )
    }

    state.pendingDeletePaymentId?.let { paymentId ->
        DeleteLoanPaymentDialog(
            payment = state.payments.find { it.paymentId == paymentId },
            isDeleting = state.isDeletingPayment,
            onConfirm = { onIntent(LoanDetailIntent.OnDeletePaymentConfirm) },
            onDismiss = { onIntent(LoanDetailIntent.OnDeletePaymentDismiss) },
        )
    }

    state.payment?.let { form ->
        LoanPaymentSheet(form = form, loanRemaining = summary?.remaining, onIntent = onIntent)
    }
}

@Preview
@Composable
private fun LoanDetailScreenPreview() {
    EmmTheme {
        LoanDetailScreen(
            state = LoanDetailUiState(
                summary = LoanSummaryUi(
                    personName = "Juan",
                    principal = "S/ 200.00",
                    interestPercentLabel = "5%",
                    totalDue = "S/ 210.00",
                    paidSoFar = "S/ 50.00",
                    remaining = "S/ 160.00",
                    isSettled = false,
                    readableLentAt = "3 de julio de 2026",
                    note = "Para el arreglo del carro",
                ),
                payments = listOf(
                    LoanPaymentRowUi(
                        paymentId = "1",
                        amount = "S/ 50.00",
                        methodLabel = "Efectivo",
                        readablePaidAt = "10 de julio de 2026",
                        note = "",
                    ),
                ),
            ),
            onIntent = {},
            onBack = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview
@Composable
private fun LoanDetailScreenEmptyPreview() {
    EmmTheme {
        LoanDetailScreen(
            state = LoanDetailUiState(
                summary = LoanSummaryUi(
                    personName = "María",
                    principal = "S/ 100.00",
                    interestPercentLabel = "0%",
                    totalDue = "S/ 100.00",
                    paidSoFar = "S/ 0.00",
                    remaining = "S/ 100.00",
                    isSettled = false,
                    readableLentAt = "1 de agosto de 2026",
                    note = "",
                ),
            ),
            onIntent = {},
            onBack = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
