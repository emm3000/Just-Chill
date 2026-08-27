package com.emm.justchill.hh.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.atoms.Pill
import com.emm.justchill.core.ui.atoms.PillTone

@Composable
fun LoanSummaryCard(summary: LoanSummaryUi, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val remainingColor = if (summary.isSettled) colors.textTertiary else colors.textPrimary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(radii.rM)
            .background(colors.surface1)
            .border(1.dp, colors.border, radii.rM)
            .padding(spacing.s4),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            Text(
                text = "Prestado el ${summary.readableLentAt}",
                style = type.caption,
                color = colors.textTertiary,
            )
            if (summary.isSettled) {
                Pill(text = "Liquidado", tone = PillTone.Pos)
            }
        }
        Spacer(Modifier.height(spacing.s2))
        Text(
            text = summary.remaining,
            style = type.amountCard,
            color = remainingColor,
        )
        Text(
            text = "Por cobrar",
            style = type.caption,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(spacing.s3))
        Hairline()
        Spacer(Modifier.height(spacing.s3))
        SummaryStatRow(label = "Prestado", value = summary.principal)
        Spacer(Modifier.height(spacing.s2))
        SummaryStatRow(label = "Interés", value = summary.interestPercentLabel)
        Spacer(Modifier.height(spacing.s2))
        SummaryStatRow(label = "Total a pagar", value = summary.totalDue)
        Spacer(Modifier.height(spacing.s2))
        SummaryStatRow(label = "Pagado", value = summary.paidSoFar)
        if (summary.note.isNotBlank()) {
            Spacer(Modifier.height(spacing.s3))
            Text(
                text = summary.note,
                style = type.bodyM.copy(fontStyle = FontStyle.Italic),
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun SummaryStatRow(label: String, value: String) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = type.bodyM, color = colors.textSecondary)
        Text(text = value, style = type.amountS, color = colors.textPrimary)
    }
}

private val activeLoanSummary = LoanSummaryUi(
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
private fun LoanSummaryCardPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg)
                .padding(16.dp),
        ) {
            LoanSummaryCard(summary = activeLoanSummary)
        }
    }
}

@Preview
@Composable
private fun LoanSummaryCardSettledPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg)
                .padding(16.dp),
        ) {
            LoanSummaryCard(
                summary = activeLoanSummary.copy(
                    paidSoFar = "S/ 1,260.00",
                    remaining = "S/ 0.00",
                    remainingCents = 0L,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun LoanSummaryCardLongNotePreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg)
                .padding(16.dp),
        ) {
            LoanSummaryCard(
                summary = activeLoanSummary.copy(
                    note = "Prestado para completar el pago del alquiler de agosto porque el " +
                        "banco demoró la transferencia del sueldo hasta el día quince",
                ),
            )
        }
    }
}
