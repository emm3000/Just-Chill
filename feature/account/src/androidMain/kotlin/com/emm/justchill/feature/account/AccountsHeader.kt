package com.emm.justchill.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.presentation.format.monthLabel
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

// The summary strip's divider; 36dp sits evenly between s8 (32dp) and s10 (40dp), so no step fits.
private val SummaryDividerHeight: Dp = 36.dp

@Composable
internal fun AccountsHeader(state: AccountsUiState, addAccount: () -> Unit) {
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Column(modifier = Modifier.fillMaxWidth()) {
        JcTopBar(
            title = "Cuentas",
            right = { NewAccountButton(onClick = addAccount) },
            column = spacing.s6,
        )

        MonthSummaryStrip(state = state)
        Hairline()
    }
}

@Composable
private fun NewAccountButton(onClick: () -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current
    val type: EmmType = LocalEmmType.current
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = spacing.s12)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = "Crear cuenta",
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .height(spacing.s10)
                .clip(radii.rFull)
                .background(colors.surface1)
                .border(spacing.hairline, colors.border, radii.rFull)
                .indication(interactionSource, ripple())
                .padding(horizontal = spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s1),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(spacing.s4),
            )
            Text(text = "Nueva", style = type.labelL, color = colors.textPrimary)
        }
    }
}

@Composable
private fun MonthSummaryStrip(state: AccountsUiState) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    SummaryStripLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = spacing.s6, end = spacing.s6, top = spacing.s6, bottom = spacing.s5),
        gap = spacing.s6,
        stackGap = spacing.s4,
        spent = {
            SummaryColumn(
                eyebrow = "Gastado en ${state.month.monthLabel()}",
                amount = state.monthSpent,
                amountColor = colors.textPrimary,
            )
        },
        divider = {
            Box(
                modifier = Modifier
                    .width(spacing.hairline)
                    .height(SummaryDividerHeight)
                    .background(colors.border),
            )
        },
        income = {
            SummaryColumn(
                eyebrow = "Ingresado",
                amount = state.monthIncome,
                amountColor = colors.textSecondary,
            )
        },
    )
}

@Composable
private fun SummaryStripLayout(
    gap: Dp,
    stackGap: Dp,
    spent: @Composable () -> Unit,
    divider: @Composable () -> Unit,
    income: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        contents = listOf(spent, divider, income),
        modifier = modifier,
    ) { measurables: List<List<Measurable>>, constraints: Constraints ->
        val gapPx: Int = gap.roundToPx()
        val columnConstraints: Constraints = Constraints(maxWidth = constraints.maxWidth)
        val spentMeasurable: Measurable = measurables[0].single()
        val incomeMeasurable: Measurable = measurables[2].single()
        val dividerPlaceable: Placeable = measurables[1].single().measure(Constraints())
        val placement: BesideOrStacked = summaryStripPlacement(
            spentWidth = spentMeasurable.maxIntrinsicWidth(Constraints.Infinity),
            dividerWidth = dividerPlaceable.width,
            incomeWidth = incomeMeasurable.maxIntrinsicWidth(Constraints.Infinity),
            gap = gapPx,
            maxWidth = constraints.maxWidth,
        )
        val spentPlaceable: Placeable = spentMeasurable.measure(columnConstraints)
        val incomePlaceable: Placeable = incomeMeasurable.measure(columnConstraints)
        if (placement == BesideOrStacked.Beside) {
            val dividerX: Int = spentPlaceable.width + gapPx
            val height: Int = maxOf(spentPlaceable.height, dividerPlaceable.height, incomePlaceable.height)
            layout(constraints.maxWidth, height) {
                spentPlaceable.placeRelative(0, 0)
                dividerPlaceable.placeRelative(dividerX, 0)
                incomePlaceable.placeRelative(dividerX + dividerPlaceable.width + gapPx, 0)
            }
        } else {
            val incomeY: Int = spentPlaceable.height + stackGap.roundToPx()
            layout(constraints.maxWidth, incomeY + incomePlaceable.height) {
                spentPlaceable.placeRelative(0, 0)
                incomePlaceable.placeRelative(0, incomeY)
            }
        }
    }
}

internal fun summaryStripPlacement(
    spentWidth: Int,
    dividerWidth: Int,
    incomeWidth: Int,
    gap: Int,
    maxWidth: Int,
): BesideOrStacked = if (spentWidth + gap + dividerWidth + gap + incomeWidth <= maxWidth) {
    BesideOrStacked.Beside
} else {
    BesideOrStacked.Stacked
}

@Composable
private fun SummaryColumn(eyebrow: String, amount: String, amountColor: Color) {
    val type: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Column(verticalArrangement = Arrangement.spacedBy(spacing.s1)) {
        Eyebrow(text = eyebrow)
        Text(text = amount, style = type.amountLead, color = amountColor, softWrap = false)
    }
}
