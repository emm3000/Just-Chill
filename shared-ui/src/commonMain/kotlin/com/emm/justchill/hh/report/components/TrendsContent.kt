package com.emm.justchill.hh.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.hh.report.TrendsUiData

/**
 * Top-level content for the Tendencias tab.
 *
 * If isEarlyState (<3 months of data), shows an empty state with guidance copy.
 * Otherwise renders: SavingsRateBlock → EntroVsSalioCard → TopExpensesCard.
 */
@Composable
fun TrendsContent(trends: TrendsUiData, modifier: Modifier = Modifier) {
    if (trends.isEarlyState) {
        TrendsEarlyState(modifier = modifier)
        return
    }

    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.s6),
    ) {
        SavingsRateBlock(
            ratePercent = trends.savingsRatePercent,
            deltaText = trends.deltaText,
            deltaIsPositive = trends.deltaIsPositive,
            contextSentence = trends.contextSentence,
        )

        EntroVsSalioCard(
            items = trends.monthlyBars,
            averageIncomeFormatted = trends.averageIncomeFormatted,
            averageExpenseFormatted = trends.averageExpenseFormatted,
        )

        if (trends.topExpenses.isNotEmpty()) {
            TopExpensesCard(items = trends.topExpenses)
        }
    }
}

@Composable
private fun TrendsEarlyState(modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.s12),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        Icon(
            imageVector = Icons.Outlined.TrendingUp,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(spacing.s2))
        Text(
            text = "Vuelve cuando tengas más historial",
            style = type.headlineM,
            color = colors.textPrimary,
        )
        Text(
            text = "Tendencias necesita al menos 3 meses para tener algo útil que mostrar.",
            style = type.bodyM,
            color = colors.textSecondary,
        )
    }
}

@Preview
@Composable
private fun TrendsEarlyStatePreview() {
    EmmTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalEmmColors.current.bg)
                .padding(16.dp),
        ) {
            TrendsContent(trends = TrendsUiData(isEarlyState = true))
        }
    }
}
