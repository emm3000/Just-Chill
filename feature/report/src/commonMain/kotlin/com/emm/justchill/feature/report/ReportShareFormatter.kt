package com.emm.justchill.feature.report

import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.presentation.format.formatNeutral
import com.emm.justchill.core.presentation.format.monthLabel

private const val PERCENT_BASE = 100

internal object ReportShareFormatter {

    fun buildContextSentence(ratePercent: Int, deltaPoints: Int?): String {
        val perHundred: String = formatNeutral(PERCENT_BASE.toString())
        val base: String = if (ratePercent < 0) {
            "De cada $perHundred que entró, gastaste ${formatNeutral((PERCENT_BASE - ratePercent).toString())}."
        } else {
            "De cada $perHundred que entró, ahorraste ${formatNeutral(ratePercent.toString())}."
        }
        if (deltaPoints == null) return base
        val comparison: String = when {
            deltaPoints > 0 -> " Mejoraste vs. los $TRENDS_WINDOW_MONTHS meses previos."
            deltaPoints < 0 -> " Empeoraste vs. los $TRENDS_WINDOW_MONTHS meses previos."
            else -> " Mantuviste el mismo ritmo que los $TRENDS_WINDOW_MONTHS meses previos."
        }
        return base + comparison
    }

    fun buildTopMetaText(monthsInTop: Int, totalMonths: Int): String = "Top en $monthsInTop de $totalMonths meses"

    fun buildMonthShareText(state: ReportUiState): String = buildString {
        appendLine("Reporte de ${state.month.monthLabel()} ${state.month.year}")
        val typeLabel: String = when (state.selectedType) {
            TransactionType.Income -> "Ingresos"
            TransactionType.Spend -> "Gastos"
        }
        appendLine("$typeLabel: ${state.totalFormatted}")
        val deltaAmt: String? = state.comparisonAmountFormatted
        val deltaPct: Int = state.comparisonPercent
        val vsText: String? = state.comparisonText
        if (deltaAmt != null && vsText != null) {
            val sign: String = when (state.comparisonDirectionUp) {
                true -> "↑ "
                false -> "↓ "
                null -> ""
            }
            appendLine("$sign$deltaAmt · ${comparisonPercentLabel(deltaPct)} $vsText")
        }
        appendLine("Por categoría:")
        state.shares.forEach { share ->
            appendLine("– ${share.name}: ${share.amountFormatted} (${share.percentage}%)")
        }
        val movText: String = "${state.movementCount} ${if (state.movementCount == 1) "movimiento" else "movimientos"}"
        appendLine("$movText · Promedio ${state.averageFormatted}")
        append("— JustChill")
    }

    fun buildTrendsShareText(state: ReportUiState): String {
        val t: TrendsUiData = state.trends
        return buildString {
            appendLine("Reporte · Tendencias $TRENDS_WINDOW_MONTHS meses")
            val deltaStr: String = t.deltaText?.let { text ->
                val sign: String = when (t.deltaIsPositive) {
                    true -> "↑ "
                    false -> "↓ "
                    null -> ""
                }
                " ($sign$text vs. $TRENDS_WINDOW_MONTHS meses previos)"
            }.orEmpty()
            appendLine("Tasa de ahorro: ${t.savingsRatePercent}%$deltaStr")
            appendLine("Promedio mensual: ingresos ${t.averageIncomeFormatted} · gastos ${t.averageExpenseFormatted}")
            if (t.topExpenses.isNotEmpty()) {
                appendLine("Mayores gastos:")
                t.topExpenses.forEach { item ->
                    appendLine("– ${item.name}: ${item.totalFormatted} (${item.topMetaText.lowercase()})")
                }
            }
            append("— JustChill")
        }
    }
}
