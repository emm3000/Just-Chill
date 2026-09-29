package com.emm.justchill.feature.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.atoms.SegmentOption
import com.emm.justchill.core.ui.atoms.Segmented
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing

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
                .padding(LocalEmmSpacing.current.s4),
        ) {
            ToggleIncomeExpense(
                selected = TransactionType.Income,
                onSelect = {},
            )
        }
    }
}
