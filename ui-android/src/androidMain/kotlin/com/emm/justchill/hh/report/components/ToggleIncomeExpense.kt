package com.emm.justchill.hh.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.SegmentOption
import com.emm.justchill.core.ui.atoms.Segmented

/**
 * Segmented control for Income / Expense.
 *
 * Pattern: Material SegmentedButtonRow (hand-rolled to control the exact
 * look — no Material defaults, full design system tokens).
 *
 * Decision D5 (PLAN_S1_REPORT.md): toggle is present from day 1.
 * Both queries share SQL — the toggle is ~50 LOC extra well spent.
 *
 * Implemented via the generic [Segmented] atom.
 */
@Composable
fun ToggleIncomeExpense(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        SegmentOption(TransactionType.Income, "Ingresos"),
        SegmentOption(TransactionType.Spend, "Gastos"),
    )
    Segmented(
        options = options,
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun ToggleIncomeExpensePreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg)
                .padding(16.dp),
        ) {
            ToggleIncomeExpense(
                selected = TransactionType.Income,
                onSelect = {},
            )
        }
    }
}
