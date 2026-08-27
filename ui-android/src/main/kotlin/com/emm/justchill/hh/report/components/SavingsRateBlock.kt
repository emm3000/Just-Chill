package com.emm.justchill.hh.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.Pill
import com.emm.justchill.core.ui.atoms.PillTone
import com.emm.justchill.hh.report.TRENDS_WINDOW_MONTHS

@Composable
fun SavingsRateBlock(
    ratePercent: Int,
    deltaText: String?,
    deltaIsPositive: Boolean?,
    contextSentence: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        Eyebrow(text = "TASA DE AHORRO · $TRENDS_WINDOW_MONTHS MESES")

        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // A negative rate keeps `danger`: it is not a spend amount but a deficit period, which
            // §4 treats as broken, not merely negative.
            val rateColor = if (ratePercent < 0) colors.danger else colors.textPrimary
            val rateAnnotated: AnnotatedString = buildAnnotatedString {
                withStyle(SpanStyle(color = rateColor)) {
                    append("$ratePercent")
                }
                withStyle(SpanStyle(color = colors.textTertiary, fontSize = type.amountL.fontSize)) {
                    append("%")
                }
            }
            Text(
                text = rateAnnotated,
                style = type.amountHero,
            )

            if (deltaText != null && deltaIsPositive != null) {
                val tone = if (deltaIsPositive) PillTone.Pos else PillTone.Neutral
                val icon = if (deltaIsPositive) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward
                Pill(
                    text = deltaText,
                    tone = tone,
                    leadingIcon = icon,
                )
            }
        }

        if (contextSentence.isNotBlank()) {
            Text(
                text = contextSentence,
                style = type.bodyM,
                color = colors.textSecondary,
            )
        }
    }
}

@Preview
@Composable
private fun SavingsRateBlockPreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg)
                .padding(16.dp),
        ) {
            SavingsRateBlock(
                ratePercent = 26,
                deltaText = "4 pts",
                deltaIsPositive = true,
                contextSentence = "De cada S/ 100 que entró, ahorraste S/ 26. Mejoraste vs. los 6 meses previos.",
            )
        }
    }
}
