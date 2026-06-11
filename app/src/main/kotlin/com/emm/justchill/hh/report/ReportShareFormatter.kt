package com.emm.justchill.hh.report

import com.emm.domain.transaction.TransactionType
import com.emm.justchill.hh.shared.shortLabel

/**
 * Stateless formatter for share-report text.
 *
 * All user-facing copy that was previously embedded in ReportViewModel lives here.
 * Every function is pure: it takes already-computed data and returns a String.
 */
internal object ReportShareFormatter {

    /**
     * Builds the "De cada S/ 100..." context sentence shown in the Trends tab.
     *
     * Examples:
     *   ratePercent=30, deltaPoints=null → "De cada S/ 100 que entró, ahorraste S/ 30."
     *   ratePercent=30, deltaPoints=5   → "De cada S/ 100 que entró, ahorraste S/ 30. Mejoraste vs. los 6 meses previos."
     *   ratePercent=30, deltaPoints=-2  → "De cada S/ 100 que entró, ahorraste S/ 30. Empeoraste vs. los 6 meses previos."
     *   ratePercent=30, deltaPoints=0   → "De cada S/ 100 que entró, ahorraste S/ 30. Mantuviste el mismo ritmo que los 6 meses previos."
     */
    fun buildContextSentence(ratePercent: Int, deltaPoints: Int?): String {
        val base = "De cada S/ 100 que entró, ahorraste S/ $ratePercent."
        if (deltaPoints == null) return base
        val comparison = when {
            deltaPoints > 0 -> " Mejoraste vs. los 6 meses previos."
            deltaPoints < 0 -> " Empeoraste vs. los 6 meses previos."
            else -> " Mantuviste el mismo ritmo que los 6 meses previos."
        }
        return base + comparison
    }

    /**
     * Builds the "Top en X de Y meses" label used in the top-expenses list.
     */
    fun buildTopMetaText(monthsInTop: Int, totalMonths: Int): String =
        "Top en $monthsInTop de $totalMonths meses"

    /** Formats the full share text for the "Mes" tab. */
    fun buildMesShareText(state: ReportUiState): String = buildString {
        appendLine("Reporte de ${state.month.shortLabel()} ${state.month.year}")
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

    /** Formats the full share text for the "Tendencias" tab. */
    fun buildTrendsShareText(state: ReportUiState): String {
        val t = state.trends
        return buildString {
            appendLine("Reporte · Tendencias 6 meses")
            val deltaStr = t.deltaText?.let { " ($it vs. 6 meses previos)" } ?: ""
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
