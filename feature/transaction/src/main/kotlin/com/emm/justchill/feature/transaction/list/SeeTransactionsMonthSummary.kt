package com.emm.justchill.feature.transaction.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.ui.atoms.AmountHero
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.format.format
import com.emm.justchill.core.ui.format.formatNeutral
import com.emm.justchill.core.ui.format.positiveMoneyFormatted
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType
import com.emm.justchill.core.ui.theme.PlexMonoFontFamily
import com.emm.justchill.core.ui.theme.edgeGiveback

private const val CENTS_PER_SOL = 100.0

@Composable
internal fun MonthStrip(
    isMonthNavigationVisible: Boolean,
    summary: MonthSummaryUi?,
    onIntent: (SeeTransactionsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = spacing.s8),
        verticalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        if (isMonthNavigationVisible) {
            MonthNavigationRow(onIntent = onIntent)
        }

        if (summary != null) {
            MonthTotals(summary = summary)
        }
    }
}

@Composable
private fun MonthNavigationRow(onIntent: (SeeTransactionsIntent) -> Unit) {
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = spacing.s6, end = spacing.s6 - spacing.edgeGiveback(spacing.s5)),
    ) {
        Spacer(Modifier.weight(1f))
        MonthChevron(
            icon = Icons.Outlined.ChevronLeft,
            contentDescription = "Mes anterior",
            onClick = { onIntent(SeeTransactionsIntent.OnPreviousMonth) },
        )
        MonthChevron(
            icon = Icons.Outlined.ChevronRight,
            contentDescription = "Mes siguiente",
            onClick = { onIntent(SeeTransactionsIntent.OnNextMonth) },
        )
    }
}

@Composable
private fun MonthChevron(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(spacing.s12)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.textSecondary,
            modifier = Modifier.size(spacing.s5),
        )
    }
}

@Composable
private fun MonthTotals(summary: MonthSummaryUi) {
    val type: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = spacing.s6, end = spacing.s6),
        verticalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        Eyebrow(text = "Gastaste este mes")
        AmountHero(value = summary.spend.cents / CENTS_PER_SOL, size = type.amountHero.fontSize)
        SecondaryLine(summary = summary)
    }
}

@Composable
private fun SecondaryLine(summary: MonthSummaryUi) {
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Row(
        modifier = Modifier.padding(top = spacing.s1),
        horizontalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        SecondaryAmount(label = "Ingresos", value = formatNeutral(summary.income.format()))
        SecondaryAmount(
            label = "Balance",
            value = summary.net.positiveMoneyFormatted(),
            tone = balanceTone(summary.net),
        )
    }
}

internal fun balanceTone(net: Money): AmountTone = if (net.cents > 0L) AmountTone.Pos else AmountTone.Neutral

@Composable
private fun SecondaryAmount(label: String, value: String, tone: AmountTone = AmountTone.Neutral) {
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current
    val valueColor: Color = if (tone == AmountTone.Pos) colors.success else colors.textSecondary

    Text(
        text = buildAnnotatedString {
            append("$label ")
            withStyle(SpanStyle(fontFamily = PlexMonoFontFamily, color = valueColor)) {
                append(value)
            }
        },
        style = type.bodyM,
        color = colors.textTertiary,
        maxLines = 1,
    )
}
