package com.emm.justchill.feature.transaction.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.ui.atoms.AmountHero
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.ChevronTrailing
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.format.format
import com.emm.justchill.core.ui.format.formatNeutral
import com.emm.justchill.core.ui.format.monthLabel
import com.emm.justchill.core.ui.format.monthYearLabel
import com.emm.justchill.core.ui.format.positiveMoneyFormatted
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType
import com.emm.justchill.core.ui.theme.PlexMonoFontFamily

@Composable
internal fun MonthHeader(
    month: YearMonth,
    currentYear: Int,
    summary: MonthSummaryUi?,
    onIntent: (SeeTransactionsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        MonthEyebrowRow(month = month, currentYear = currentYear, onIntent = onIntent)

        if (summary != null) {
            MonthTotals(summary = summary)
        }
    }
}

// The chevron marks a sheet, not a pushed screen (ADR 022): the eyebrow row is the one door into
// the month picker, so it keeps the affordance the row-chevron rule reserves for navigation.
@Composable
private fun MonthEyebrowRow(month: YearMonth, currentYear: Int, onIntent: (SeeTransactionsIntent) -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    val background: Color = if (isPressed) colors.surface1 else Color.Transparent
    val eyebrowText: String = if (month.year == currentYear) {
        "Gastado en ${month.monthLabel()}"
    } else {
        "Gastado en ${month.monthLabel()} ${month.year}"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.s12)
            .background(background)
            .clickable(interactionSource = interactionSource, indication = null) {
                onIntent(SeeTransactionsIntent.ScreenChromeIntent.OnMonthPickerRequested)
            }
            .padding(horizontal = spacing.s6)
            .semantics {
                contentDescription = "Gastado en ${month.monthYearLabel()}. Cambiar de mes"
                role = Role.Button
            },
    ) {
        Eyebrow(text = eyebrowText)
        Spacer(Modifier.width(spacing.s1))
        ChevronTrailing()
    }
}

@Composable
private fun MonthTotals(summary: MonthSummaryUi) {
    val type: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s6),
        verticalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        AmountHero(value = summary.spend.cents / CENTS_PER_SOL, size = type.amountL.fontSize)
        SecondaryLine(summary = summary)
    }
}

@Composable
private fun SecondaryLine(summary: MonthSummaryUi) {
    val spacing: EmmSpacing = LocalEmmSpacing.current

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.s4),
        verticalArrangement = Arrangement.spacedBy(spacing.s1),
    ) {
        SecondaryAmount(label = "Entró", value = formatNeutral(summary.income.format()))
        SecondaryAmount(
            label = "Neto",
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

private const val CENTS_PER_SOL = 100.0
