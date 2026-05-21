package com.emm.justchill.hh.report

import androidx.compose.ui.graphics.Color
import com.emm.domain.report.CategoryAggregate
import com.emm.domain.report.CategoryAmount
import com.emm.domain.report.GetMonthlyAmountByCategoryUseCase
import com.emm.domain.report.GetMonthlyComparisonUseCase
import com.emm.domain.report.GetMonthlySectionStatsUseCase
import com.emm.domain.report.GetSavingsRateUseCase
import com.emm.domain.report.GetTopCategoriesOverMonthsUseCase
import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.core.theme.emmDarkColors
import com.emm.justchill.hh.category.AppIconCatalog
import com.emm.justchill.hh.shared.shortLabel
import com.emm.justchill.hh.shared.shortLabel3
import java.text.NumberFormat
import java.util.Locale

class ReportViewModel(
    private val getMonthlyAmountByCategory: GetMonthlyAmountByCategoryUseCase,
    private val getMonthlyComparison: GetMonthlyComparisonUseCase,
    private val getMonthlySectionStats: GetMonthlySectionStatsUseCase,
    private val getSavingsRate: GetSavingsRateUseCase,
    private val getTopCategories: GetTopCategoriesOverMonthsUseCase,
) : MviViewModel<ReportUiState, ReportIntent, ReportEffect>() {

    override val initialState: ReportUiState =
        ReportUiState(month = YearMonth.current(), selectedType = TransactionType.Income)

    init {
        loadReport()
        loadTrends()
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

            is ReportIntent.SelectTab -> {
                updateState { copy(selectedTab = intent.tab) }
                when (intent.tab) {
                    ReportTab.Mes -> loadReport()
                    ReportTab.Tendencias -> loadTrends()
                }
            }

            ReportIntent.ShareReport -> buildAndShareReport()
        }
    }

    private fun loadReport() {
        val month = currentState.month
        val type = currentState.selectedType
        launchSafe(onError = { e -> ReportEffect.ShowError(e.toUserMessage()) }) {
            val amounts: List<CategoryAmount> = getMonthlyAmountByCategory(month, type)
            val comparison = getMonthlyComparison(month, type)
            val stats = getMonthlySectionStats(month, type)

            val total: Money = amounts.fold(Money.Zero) { acc, item -> acc + item.amount }

            val comparisonText = comparison?.let { "vs ${month.previous().shortLabel()}" }
            val comparisonAmountFormatted = comparison?.let { mc ->
                val abs = if (mc.absoluteDelta.cents < 0) -mc.absoluteDelta else mc.absoluteDelta
                formatSoles(abs.cents)
            }

            val shares = buildShares(amounts, total)

            updateState {
                copy(
                    totalFormatted = formatSolesWithDecimals(total.cents),
                    comparisonText = comparisonText,
                    comparisonIsPositive = comparison?.let { it.deltaPercent >= 0 },
                    comparisonAmountFormatted = comparisonAmountFormatted,
                    comparisonPercent = comparison?.deltaPercent ?: 0,
                    shares = shares,
                    isEmpty = amounts.isEmpty(),
                    movementCount = stats.movementCount,
                    averageFormatted = formatSoles(stats.averageAmount.cents),
                )
            }
        }
    }

    private fun loadTrends() {
        launchSafe(onError = { e -> ReportEffect.ShowError(e.toUserMessage()) }) {
            val savingsRate = getSavingsRate(months = 6)
            val topExpenses = getTopCategories(TransactionType.Spend, months = 6, topN = 3)

            val currentYm = YearMonth.current()
            val monthsWithData = savingsRate.monthly.count { m ->
                m.income.cents > 0 || m.expense.cents > 0
            }
            val isEarlyState = monthsWithData < 3

            val deltaText = savingsRate.deltaPointsVsPrior?.let { delta ->
                val sign = if (delta >= 0) "↑" else "↓"
                "$sign ${Math.abs(delta)} pts"
            }
            val deltaIsPositive = savingsRate.deltaPointsVsPrior?.let { it >= 0 }

            val contextSentence = buildContextSentence(
                ratePercent = savingsRate.currentRatePercent,
                deltaPoints = savingsRate.deltaPointsVsPrior,
            )

            val barItems = savingsRate.monthly.map { m ->
                MonthlyBarItem(
                    monthShortLabel = m.yearMonth.shortLabel3(),
                    isCurrentMonth = m.yearMonth == currentYm,
                    incomeAmount = m.income.cents,
                    expenseAmount = m.expense.cents,
                    incomeFormatted = formatSoles(m.income.cents),
                    expenseFormatted = formatSoles(m.expense.cents),
                )
            }

            val topItems = topExpenses.map { it.toTopCategoryItem() }

            updateState {
                copy(
                    trends = TrendsUiData(
                        savingsRatePercent = savingsRate.currentRatePercent,
                        deltaText = deltaText,
                        deltaIsPositive = deltaIsPositive,
                        contextSentence = contextSentence,
                        monthlyBars = barItems,
                        averageIncomeFormatted = formatSoles(savingsRate.averageIncome.cents),
                        averageExpenseFormatted = formatSoles(savingsRate.averageExpense.cents),
                        topExpenses = topItems,
                        isEarlyState = isEarlyState,
                    ),
                )
            }
        }
    }

    private fun buildContextSentence(ratePercent: Int, deltaPoints: Int?): String {
        val saved = ratePercent
        val base = "De cada S/ 100 que entró, ahorraste S/ $saved."
        if (deltaPoints == null) return base
        val comparison = when {
            deltaPoints > 0 -> " Mejoraste vs. los 6 meses previos."
            deltaPoints < 0 -> " Empeoraste vs. los 6 meses previos."
            else -> " Mantuviste el mismo ritmo que los 6 meses previos."
        }
        return base + comparison
    }

    private fun buildAndShareReport() {
        val state = currentState
        val text = when (state.selectedTab) {
            ReportTab.Mes -> buildMesShareText(state)
            ReportTab.Tendencias -> buildTrendsShareText(state)
        }
        sendEffect(ReportEffect.ShareReport(text))
    }

    private fun buildMesShareText(state: ReportUiState): String = buildString {
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
            val sign = if (state.comparisonIsPositive == true) "↑" else "↓"
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

    private fun buildTrendsShareText(state: ReportUiState): String {
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

    private fun CategoryAggregate.toTopCategoryItem(): TopCategoryItem {
        val icon = AppIconCatalog.findById(categoryIcon)
        val topMeta = "Top en $monthsInTop de $totalMonths meses"
        return TopCategoryItem(
            categoryId = categoryId.value,
            name = categoryName,
            iconKey = categoryIcon,
            tint = domainColorToUi(categoryColor),
            totalFormatted = formatSoles(totalAmount.cents),
            topMetaText = topMeta,
        )
    }

    private fun formatSoles(cents: Long): String {
        val soles = cents.toDouble() / 100.0
        val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-PE"))
        nf.minimumFractionDigits = 0
        nf.maximumFractionDigits = 0
        return "S/ ${nf.format(soles)}"
    }

    private fun formatSolesWithDecimals(cents: Long): String {
        val soles = cents.toDouble() / 100.0
        val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-PE"))
        nf.minimumFractionDigits = 2
        nf.maximumFractionDigits = 2
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
