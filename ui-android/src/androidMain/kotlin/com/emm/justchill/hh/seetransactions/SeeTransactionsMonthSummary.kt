package com.emm.justchill.hh.seetransactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.MoneyInline
import com.emm.justchill.core.ui.atoms.MonthSelector
import com.emm.justchill.hh.shared.monthYearLabel

@Composable
internal fun MonthSection(state: SeeTransactionsUiState, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        MonthSelector(
            label = state.month.monthYearLabel(),
            onPrevious = onPreviousMonth,
            onNext = onNextMonth,
        )
    }

    val summary = state.summary
    if (summary != null && state.listDisplayState == ListDisplayState.Content) {
        MonthSummaryStrip(summary = summary)
    }
}

@Composable
private fun MonthSummaryStrip(summary: MonthSummaryUi) {
    val colors = LocalEmmColors.current

    val netCents = summary.net.cents
    val netColor = when {
        netCents > 0L -> colors.success
        netCents < 0L -> colors.textPrimary
        else -> colors.textSecondary
    }

    Row(
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SummaryCell(
            label = "Ingresos",
            value = summary.income.cents.toDouble() / 100.0,
            valueColor = colors.textPrimary,
        )
        SummaryDivider()
        SummaryCell(
            label = "Gastos",
            value = summary.spend.cents.toDouble() / 100.0,
            valueColor = colors.textSecondary,
        )
        SummaryDivider()
        SummaryCell(
            label = "Balance",
            value = netCents.toDouble() / 100.0,
            valueColor = netColor,
        )
    }
}

@Composable
private fun SummaryCell(label: String, value: Double, valueColor: Color) {
    val colors = LocalEmmColors.current
    Column {
        Eyebrow(text = label, color = colors.textDisabled)
        Spacer(Modifier.height(4.dp))
        MoneyInline(value = value, color = valueColor)
    }
}

@Composable
private fun SummaryDivider() {
    val colors = LocalEmmColors.current
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(colors.border),
    )
}
