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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.format.monthLabel
import com.emm.justchill.core.ui.theme.EmmSpacing
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
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    val radii = LocalEmmRadii.current
    val type = LocalEmmType.current
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

/**
 * The month's spend is this screen's one hero; income shares its size and steps down a tone
 * rather than competing for the glance.
 */
@Composable
private fun MonthSummaryStrip(state: AccountsUiState) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = spacing.s6, end = spacing.s6, top = spacing.s6, bottom = spacing.s5),
        horizontalArrangement = Arrangement.spacedBy(spacing.s6),
    ) {
        SummaryColumn(
            eyebrow = "Gastado en ${state.month.monthLabel()}",
            amount = state.monthSpent,
            amountColor = colors.textPrimary,
        )
        Box(
            modifier = Modifier
                .width(spacing.hairline)
                .height(SummaryDividerHeight)
                .background(colors.border),
        )
        SummaryColumn(
            eyebrow = "Ingresado",
            amount = state.monthIncome,
            amountColor = colors.textSecondary,
        )
    }
}

@Composable
private fun SummaryColumn(eyebrow: String, amount: String, amountColor: Color) {
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(verticalArrangement = Arrangement.spacedBy(spacing.s1)) {
        Eyebrow(text = eyebrow)
        Text(text = amount, style = type.amountLead, color = amountColor)
    }
}
