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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.atoms.Pill
import com.emm.justchill.core.ui.atoms.PillTone

@Composable
fun LoanSummaryCard(summary: LoanSummaryUi, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val remainingColor = if (summary.isSettled) colors.textTertiary else colors.textPrimary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(radii.rM)
            .background(colors.surface1)
            .border(1.dp, colors.border, radii.rM)
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Prestado el ${summary.readableLentAt}",
                fontSize = 12.sp,
                fontFamily = InterFontFamily,
                color = colors.textTertiary,
            )
            if (summary.isSettled) {
                Pill(text = "Liquidado", tone = PillTone.Pos)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = summary.remaining,
            fontSize = 32.sp,
            fontWeight = FontWeight.W700,
            fontFamily = InterFontFamily,
            color = remainingColor,
            letterSpacing = (-0.5).sp,
        )
        Text(
            text = "Por cobrar",
            fontSize = 12.sp,
            fontFamily = InterFontFamily,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(12.dp))
        Hairline()
        Spacer(Modifier.height(12.dp))
        SummaryStatRow(label = "Prestado", value = summary.principal)
        Spacer(Modifier.height(6.dp))
        SummaryStatRow(label = "Interés", value = summary.interestPercentLabel)
        Spacer(Modifier.height(6.dp))
        SummaryStatRow(label = "Total a pagar", value = summary.totalDue)
        Spacer(Modifier.height(6.dp))
        SummaryStatRow(label = "Pagado", value = summary.paidSoFar)
        if (summary.note.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = summary.note,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = InterFontFamily,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun SummaryStatRow(label: String, value: String) {
    val colors = LocalEmmColors.current

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, fontSize = 13.sp, fontFamily = InterFontFamily, color = colors.textSecondary)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            color = colors.textPrimary,
        )
    }
}

private val activeLoanSummary = LoanSummaryUi(
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
                    isSettled = true,
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
