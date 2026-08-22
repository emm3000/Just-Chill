package com.emm.justchill.hh.loan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.atoms.IconBtn
import com.emm.justchill.core.ui.atoms.IconBtnTone
import com.emm.justchill.core.ui.atoms.Pill
import com.emm.justchill.core.ui.atoms.PillTone

@Composable
fun LoanPaymentsList(
    payments: List<LoanPaymentRowUi>,
    onEditClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (payments.isEmpty()) {
        LoanPaymentsEmptyState(modifier = modifier)
        return
    }

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 12.dp)) {
        items(payments, key = { it.paymentId }) { payment ->
            LoanPaymentRow(
                payment = payment,
                onEditClick = { onEditClick(payment.paymentId) },
                onDeleteClick = { onDeleteClick(payment.paymentId) },
            )
        }
    }
}

@Composable
private fun LoanPaymentRow(payment: LoanPaymentRowUi, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    val colors = LocalEmmColors.current

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = payment.readablePaidAt,
                        fontSize = 12.sp,
                        fontFamily = InterFontFamily,
                        color = colors.textTertiary,
                    )
                    Pill(text = payment.methodLabel, tone = PillTone.Neutral)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = payment.amount,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W700,
                    fontFamily = InterFontFamily,
                    color = colors.textPrimary,
                    letterSpacing = (-0.2).sp,
                )
                if (payment.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = payment.note,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        fontFamily = InterFontFamily,
                        color = colors.textSecondary,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconBtn(
                    icon = Icons.Outlined.Edit,
                    onClick = onEditClick,
                    contentDescription = "Editar abono",
                )
                IconBtn(
                    icon = Icons.Outlined.Delete,
                    tone = IconBtnTone.Danger,
                    onClick = onDeleteClick,
                    contentDescription = "Eliminar abono",
                )
            }
        }
        Hairline()
    }
}

@Composable
private fun LoanPaymentsEmptyState(modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current

    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Payments,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Sin abonos todavía",
            fontSize = 16.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Los abonos que registres aparecen aquí",
            fontSize = 13.sp,
            fontFamily = InterFontFamily,
            color = colors.textSecondary,
        )
    }
}
