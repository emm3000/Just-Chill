package com.emm.justchill.hh.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmType

/**
 * Segmented control for Income / Expense.
 *
 * Pattern: Material SegmentedButtonRow (hand-rolled to control the exact
 * look — no Material defaults, full design system tokens).
 *
 * Decision D5 (PLAN_S1_REPORT.md): toggle is present from day 1.
 * Both queries share SQL — the toggle is ~50 LOC extra well spent.
 */
@Composable
fun ToggleIncomeExpense(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val shape = RoundedCornerShape(6.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .border(width = 1.dp, color = colors.border, shape = shape),
    ) {
        SegmentButton(
            label = "Ingresos",
            isSelected = selected == TransactionType.Income,
            onClick = { onSelect(TransactionType.Income) },
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(colors.border),
        )
        SegmentButton(
            label = "Gastos",
            isSelected = selected == TransactionType.Spend,
            onClick = { onSelect(TransactionType.Spend) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SegmentButton(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    val bg: Color = if (isSelected) colors.accent else Color.Transparent
    val textColor: Color = if (isSelected) colors.textOnAccent else colors.textSecondary

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(bg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = type.labelL,
            color = textColor,
        )
    }
}

@PreviewLightDark
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
