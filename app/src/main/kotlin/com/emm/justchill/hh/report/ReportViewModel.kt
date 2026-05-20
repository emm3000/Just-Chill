package com.emm.justchill.hh.report

import androidx.compose.ui.graphics.Color
import com.emm.domain.report.CategoryAmount
import com.emm.domain.report.GetMonthlyAmountByCategoryUseCase
import com.emm.domain.report.GetMonthlyComparisonUseCase
import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.core.theme.emmDarkColors
import com.emm.justchill.hh.shared.shortLabel
import java.text.NumberFormat
import java.util.Locale

class ReportViewModel(
    private val getMonthlyAmountByCategory: GetMonthlyAmountByCategoryUseCase,
    private val getMonthlyComparison: GetMonthlyComparisonUseCase,
) : MviViewModel<ReportUiState, ReportIntent, ReportEffect>() {

    override val initialState: ReportUiState =
        ReportUiState(month = YearMonth.current(), selectedType = TransactionType.Income)

    init {
        loadReport()
    }

    override fun onIntent(intent: ReportIntent) {
        when (intent) {
            ReportIntent.PreviousMonth -> {
                updateState { copy(month = month.previous()) }
                loadReport()
            }

            ReportIntent.NextMonth -> {
                updateState { copy(month = month.next()) }
                loadReport()
            }

            ReportIntent.JumpToCurrent -> {
                updateState { copy(month = YearMonth.current()) }
                loadReport()
            }

            is ReportIntent.SelectType -> {
                updateState { copy(selectedType = intent.type) }
                loadReport()
            }
        }
    }

    private fun loadReport() {
        val month = currentState.month
        val type = currentState.selectedType
        launchSafe(onError = { e -> ReportEffect.ShowError(e.toUserMessage()) }) {
            val amounts: List<CategoryAmount> = getMonthlyAmountByCategory(month, type)
            val comparison = getMonthlyComparison(month, type)

            val total: Money = amounts.fold(Money.Zero) { acc, item -> acc + item.amount }

            val comparisonText = comparison?.let { mc ->
                val sign = if (mc.deltaPercent >= 0) "+" else ""
                "$sign${mc.deltaPercent}% vs ${month.previous().shortLabel()}"
            }

            val shares = buildShares(amounts, total)

            updateState {
                copy(
                    totalFormatted = formatSoles(total.cents),
                    comparisonText = comparisonText,
                    comparisonIsPositive = comparison?.let { it.deltaPercent >= 0 },
                    shares = shares,
                    isEmpty = amounts.isEmpty(),
                )
            }
        }
    }

    private fun buildShares(amounts: List<CategoryAmount>, total: Money): List<CategoryShare> {
        if (total.cents == 0L) return amounts.map { it.toCategoryShare(percentage = 0) }
        return amounts.map { item ->
            val pct = ((item.amount.cents.toDouble() / total.cents.toDouble()) * 100).toInt()
            item.toCategoryShare(percentage = pct)
        }
    }

    private fun CategoryAmount.toCategoryShare(percentage: Int) = CategoryShare(
        categoryId = categoryId.value,
        name = categoryName,
        amountFormatted = formatSoles(amount.cents),
        percentage = percentage,
        tint = domainColorToUi(categoryColor),
    )

    private fun formatSoles(cents: Long): String {
        val soles = cents.toDouble() / 100.0
        val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-PE"))
        nf.minimumFractionDigits = 0
        nf.maximumFractionDigits = 0
        return "S/ ${nf.format(soles)}"
    }

    private fun domainColorToUi(color: String): Color = when (color) {
        "green" -> emmDarkColors.catSage
        "blue" -> emmDarkColors.catSlate
        "purple" -> emmDarkColors.catMauve
        "orange" -> emmDarkColors.catOchre
        "red" -> emmDarkColors.catTerracotta
        "brown" -> emmDarkColors.catTerracotta
        "yellow" -> emmDarkColors.catOchre
        "teal" -> emmDarkColors.catSage
        "pink" -> emmDarkColors.catMauve
        "gray" -> emmDarkColors.catGraphite
        else -> emmDarkColors.catGraphite
    }
}
