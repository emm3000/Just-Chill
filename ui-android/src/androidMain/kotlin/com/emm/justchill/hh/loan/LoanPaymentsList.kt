package com.emm.justchill.hh.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.EmmRowMenu
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.atoms.Pill
import com.emm.justchill.core.ui.atoms.PillTone
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth

// Items rather than a composable: the detail screen owns the only scroll, so the abonos cannot
// carry a lazy list of their own without nesting one inside it.
internal fun LazyListScope.loanPaymentItems(
    payments: List<LoanPaymentRowUi>,
    onEditClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
) {
    if (payments.isEmpty()) {
        item {
            val spacing = LocalEmmSpacing.current
            LoanPaymentsEmptyState(modifier = Modifier.fillMaxWidth().padding(vertical = spacing.s6))
        }
        return
    }

    items(payments, key = { it.paymentId }) { payment ->
        LoanPaymentRow(
            payment = payment,
            onEditClick = { onEditClick(payment.paymentId) },
            onDeleteClick = { onDeleteClick(payment.paymentId) },
        )
    }
}

@Composable
private fun LoanPaymentRow(payment: LoanPaymentRowUi, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4, vertical = spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s3),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    Text(
                        text = payment.readablePaidAt,
                        style = type.caption,
                        color = colors.textTertiary,
                    )
                    Pill(text = payment.methodLabel, tone = PillTone.Neutral)
                }
                Spacer(Modifier.height(spacing.s1))
                Text(
                    text = payment.amount,
                    style = type.amountLead,
                    color = colors.textPrimary,
                )
                if (payment.note.isNotBlank()) {
                    Spacer(Modifier.height(spacing.s1))
                    Text(
                        text = payment.note,
                        style = type.bodyM.copy(fontStyle = FontStyle.Italic),
                        color = colors.textSecondary,
                    )
                }
            }

            EmmRowMenu(contentDescription = "Opciones del abono", onEdit = onEditClick, onDelete = onDeleteClick)
        }
        Hairline()
    }
}

@Composable
private fun LoanPaymentsEmptyState(modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier.padding(horizontal = spacing.s6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Payments,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(spacing.s3))
        Text(
            text = "Sin abonos todavía",
            style = type.titleM,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(spacing.s1))
        Text(
            text = "Los abonos que registres aparecen aquí",
            style = type.bodyM,
            color = colors.textSecondary,
        )
    }
}

private val previewLoanPayments = listOf(
    LoanPaymentRowUi(
        paymentId = "1",
        amount = "S/ 300.00",
        methodLabel = "Yape",
        readablePaidAt = "15 de agosto de 2026",
        note = "Primer abono",
    ),
    LoanPaymentRowUi(
        paymentId = "2",
        amount = "S/ 200.00",
        methodLabel = "Efectivo",
        readablePaidAt = "20 de agosto de 2026",
        note = "",
    ),
)

@Preview
@PreviewRedmi15CWidth
@Composable
private fun LoanPaymentItemsPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(LocalEmmColors.current.bg),
        ) {
            LazyColumn { loanPaymentItems(payments = previewLoanPayments, onEditClick = {}, onDeleteClick = {}) }
        }
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun LoanPaymentItemsEmptyPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(LocalEmmColors.current.bg),
        ) {
            LazyColumn { loanPaymentItems(payments = emptyList(), onEditClick = {}, onDeleteClick = {}) }
        }
    }
}
