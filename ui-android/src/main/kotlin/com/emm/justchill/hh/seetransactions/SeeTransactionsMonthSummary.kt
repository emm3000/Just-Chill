package com.emm.justchill.hh.seetransactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.theme.PlexMonoFontFamily
import com.emm.justchill.core.ui.atoms.AmountHero
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.hh.shared.formatNeutral
import com.emm.justchill.hh.shared.fromCentsToSolesWith
import com.emm.justchill.hh.shared.positiveMoneyFormatted

private const val CENTS_PER_SOL = 100.0

/**
 * One hero per screen: the month's spend takes the amount role, and income and balance step down
 * to a single line under it.
 */
@Composable
internal fun MonthSummary(summary: MonthSummaryUi, modifier: Modifier = Modifier) {
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = spacing.s6, end = spacing.s6, top = spacing.s8),
        verticalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        Eyebrow(text = "Gastaste este mes")
        AmountHero(value = summary.spend.cents / CENTS_PER_SOL, size = type.amountHero.fontSize)
        SecondaryLine(summary = summary)
    }
}

@Composable
private fun SecondaryLine(summary: MonthSummaryUi) {
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = Modifier.padding(top = spacing.s1),
        horizontalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        SecondaryAmount(label = "Ingresos", value = formatNeutral(fromCentsToSolesWith(summary.income)))
        SecondaryAmount(
            label = "Balance",
            value = summary.net.positiveMoneyFormatted(),
            tone = balanceTone(summary.net),
        )
    }
}

/**
 * `net` is a month balance, so a positive one is positive money — `success`; zero or negative
 * keeps this line's own monochrome (`textSecondary`, not a row's `textPrimary`).
 */
internal fun balanceTone(net: Money): AmountTone = if (net.cents > 0L) AmountTone.Pos else AmountTone.Neutral

@Composable
private fun SecondaryAmount(label: String, value: String, tone: AmountTone = AmountTone.Neutral) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val valueColor = if (tone == AmountTone.Pos) colors.success else colors.textSecondary

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
