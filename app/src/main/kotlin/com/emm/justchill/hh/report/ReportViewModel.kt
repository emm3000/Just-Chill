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
import com.emm.justchill.hh.shared.shortLabel
import com.emm.justchill.hh.shared.shortLabel3
import kotlinx.coroutines.Job
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

class ReportViewModel(
    private val getMonthlyAmountByCategory: GetMonthlyAmountByCategoryUseCase,
    private val getMonthlyComparison: GetMonthlyComparisonUseCase,
    private val getMonthlySectionStats: GetMonthlySectionStatsUseCase,
    private val getSavingsRate: GetSavingsRateUseCase,
    private val getTopCategories: GetTopCategoriesOverMonthsUseCase,
) : MviViewModel<ReportUiState, ReportIntent, ReportEffect>() {

    override val initialState: ReportUiState =
        ReportUiState(month = YearMonth.current(), selectedType = TransactionType.Income)

    // Latest-wins jobs: cancels the in-flight load before starting the new one.
    private var reportJob: Job? = null
    private var trendsJob: Job? = null

    init {
        reloadReport()
        reloadTrends()
    }

    // Pure dispatch — exhaustiveness enforced by compiler via expression form.
    override fun onIntent(intent: ReportIntent) = when (intent) {
        ReportIntent.PreviousMonth -> onPreviousMonth()
        ReportIntent.NextMonth -> onNextMonth()
        ReportIntent.JumpToCurrent -> onJumpToCurrent()
        is ReportIntent.SelectType -> onSelectType(intent.type)
        is ReportIntent.SelectTab -> onSelectTab(intent.tab)
        is ReportIntent.SelectMonth -> onSelectMonth(intent.month)
        ReportIntent.ShareReport -> buildAndShareReport()
    }

    // ── Intent handlers ───────────────────────────────────────────────────

    private fun onPreviousMonth() {
        updateState { copy(month = month.previous()) }
        reloadReport()
    }

    private fun onNextMonth() {
        updateState { copy(month = month.next()) }
        reloadReport()
    }

    private fun onJumpToCurrent() {
        updateState { copy(month = YearMonth.current()) }
        reloadReport()
    }

    private fun onSelectType(type: TransactionType) {
        updateState { copy(selectedType = type) }
        reloadReport()
    }

    private fun onSelectTab(tab: ReportTab) {
        updateState { copy(selectedTab = tab) }
        when (tab) {
            ReportTab.Mes -> reloadReport()
            ReportTab.Tendencias -> reloadTrends()
        }
    }

    private fun onSelectMonth(month: YearMonth) {
        updateState { copy(month = month) }
        reloadReport()
    }

    // ── Data loading (latest-wins) ────────────────────────────────────────

    private fun reloadReport() {
        reportJob?.cancel()
        reportJob = launchSafe(onError = { e -> ReportEffect.ShowError(e.toUserMessage()) }) {
            val month = currentState.month
            val type = currentState.selectedType

            val incomeAmounts: List<CategoryAmount> = getMonthlyAmountByCategory(month, TransactionType.Income)
            val spendAmounts: List<CategoryAmount> = getMonthlyAmountByCategory(month, TransactionType.Spend)
            val isMonthEmpty = incomeAmounts.isEmpty() && spendAmounts.isEmpty()

            val amounts = if (type == TransactionType.Income) incomeAmounts else spendAmounts
            val comparison = getMonthlyComparison(month, type)
            val stats = getMonthlySectionStats(month, type)

            val total: Money = amounts.fold(Money.Zero) { acc, item -> acc + item.amount }

            val comparisonText = comparison?.let { "vs ${month.previous().shortLabel()}" }
            val comparisonAmountFormatted = comparison?.let { mc ->
                val abs = if (mc.absoluteDelta.cents < 0) -mc.absoluteDelta else mc.absoluteDelta
                formatSoles(abs.cents)
            }
            val directionUp = comparison?.let { it.deltaPercent >= 0 }
            val isPositive = comparison?.let { mc ->
                when (type) {
                    TransactionType.Income -> mc.deltaPercent >= 0
                    TransactionType.Spend -> mc.deltaPercent <= 0
                }
            }

            val shares = buildShares(amounts, total)

            updateState {
                copy(
                    totalFormatted = formatSolesWithDecimals(total.cents),
                    comparisonText = comparisonText,
                    comparisonDirectionUp = directionUp,
                    comparisonIsPositive = isPositive,
                    comparisonAmountFormatted = comparisonAmountFormatted,
                    comparisonPercent = comparison?.deltaPercent ?: 0,
                    shares = shares,
                    isEmpty = amounts.isEmpty(),
                    isMonthEmpty = isMonthEmpty,
                    movementCount = stats.movementCount,
                    averageFormatted = formatSoles(stats.averageAmount.cents),
                )
            }
        }
    }

    private fun reloadTrends() {
        trendsJob?.cancel()
        trendsJob = launchSafe(onError = { e -> ReportEffect.ShowError(e.toUserMessage()) }) {
            val savingsRate = getSavingsRate(months = 6)
            val topExpenses = getTopCategories(TransactionType.Spend, months = 6, topN = 3)

            val currentYm = YearMonth.current()
            val monthsWithData = savingsRate.monthly.count { m ->
                m.income.cents > 0 || m.expense.cents > 0
            }
            val isEarlyState = monthsWithData < 3

            val deltaText = savingsRate.deltaPointsVsPrior?.let { delta ->
                val sign = if (delta >= 0) "↑" else "↓"
                "$sign ${abs(delta)} pts"
            }
            val deltaIsPositive = savingsRate.deltaPointsVsPrior?.let { it >= 0 }

            val contextSentence = ReportShareFormatter.buildContextSentence(
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

    // ── Share ─────────────────────────────────────────────────────────────

    private fun buildAndShareReport() {
        val state = currentState
        val text = when (state.selectedTab) {
            ReportTab.Mes -> ReportShareFormatter.buildMesShareText(state)
            ReportTab.Tendencias -> ReportShareFormatter.buildTrendsShareText(state)
        }
        sendEffect(ReportEffect.ShareReport(text))
    }

    // ── Mapping helpers ───────────────────────────────────────────────────

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
        return TopCategoryItem(
            categoryId = categoryId.value,
            name = categoryName,
            iconKey = categoryIcon,
            tint = domainColorToUi(categoryColor),
            totalFormatted = formatSoles(totalAmount.cents),
            topMetaText = ReportShareFormatter.buildTopMetaText(monthsInTop, totalMonths),
        )
    }

    companion object {

        fun formatSoles(cents: Long): String {
            val soles = cents.toDouble() / 100.0
            val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-PE"))
            nf.minimumFractionDigits = 0
            nf.maximumFractionDigits = 0
            return "S/ ${nf.format(soles)}"
        }

        fun formatSolesWithDecimals(cents: Long): String {
            val soles = cents.toDouble() / 100.0
            val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-PE"))
            nf.minimumFractionDigits = 2
            nf.maximumFractionDigits = 2
            return "S/ ${nf.format(soles)}"
        }

        fun domainColorToUi(color: String): Color = when (color) {
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
}
