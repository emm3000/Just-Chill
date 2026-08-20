package com.emm.justchill.hh.report

import com.emm.domain.transaction.TransactionType
import com.emm.justchill.hh.shared.monthLabel

private const val PERCENT_BASE = 100

internal object ReportShareFormatter {

    /** At -50%, `PERCENT_BASE - ratePercent` adds: the user spent 150 per 100 earned. */
    fun buildContextSentence(ratePercent: Int, deltaPoints: Int?): String {
        val base = if (ratePercent < 0) {
            "De cada S/ 100 que entró, gastaste S/ ${PERCENT_BASE - ratePercent}."
        } else {
            "De cada S/ 100 que entró, ahorraste S/ $ratePercent."
        }
        if (deltaPoints == null) return base
        val comparison = when {
            deltaPoints > 0 -> " Mejoraste vs. los $TRENDS_WINDOW_MONTHS meses previos."
            deltaPoints < 0 -> " Empeoraste vs. los $TRENDS_WINDOW_MONTHS meses previos."
            else -> " Mantuviste el mismo ritmo que los $TRENDS_WINDOW_MONTHS meses previos."
        }
        return base + comparison
    }

    fun buildTopMetaText(monthsInTop: Int, totalMonths: Int): String = "Top en $monthsInTop de $totalMonths meses"

    fun buildMesShareText(state: ReportUiState): String = buildString {
        appendLine("Reporte de ${state.month.monthLabel()} ${state.month.year}")
        val typeLabel = when (state.selectedType) {
            TransactionType.Income -> "Ingresos"
            TransactionType.Spend -> "Gastos"
        }
        appendLine("$typeLabel: ${state.totalFormatted}")
        val deltaAmt = state.comparisonAmountFormatted
        val deltaPct = state.comparisonPercent
        val vsText = state.comparisonText
        if (deltaAmt != null && vsText != null) {
            val sign = if (state.comparisonDirectionUp == true) "↑" else "↓"
            appendLine("$sign $deltaAmt · $deltaPct% $vsText")
        }
        appendLine("Por categoría:")
        state.shares.forEach { share ->
            appendLine("– ${share.name}: ${share.amountFormatted} (${share.percentage}%)")
        }
        val movText = "${state.movementCount} ${if (state.movementCount == 1) "movimiento" else "movimientos"}"
        appendLine("$movText · Promedio ${state.averageFormatted}")
        append("— JustChill")
    }

    fun buildTrendsShareText(state: ReportUiState): String {
        val t = state.trends
        return buildString {
            appendLine("Reporte · Tendencias $TRENDS_WINDOW_MONTHS meses")
            // Shared text has no pill and no icon, so the arrow the screen draws has to be
            // written out here or the reader cannot tell an improvement from a slip.
            val deltaStr = t.deltaText?.let { text ->
                val sign = if (t.deltaIsPositive == true) "↑" else "↓"
                " ($sign $text vs. $TRENDS_WINDOW_MONTHS meses previos)"
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
