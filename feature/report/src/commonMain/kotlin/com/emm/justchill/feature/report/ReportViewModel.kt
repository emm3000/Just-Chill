package com.emm.justchill.feature.report

import androidx.lifecycle.viewModelScope
import com.emm.justchill.core.domain.report.CategoryAmount
import com.emm.justchill.core.domain.report.GetMonthlyAmountByCategoryUseCase
import com.emm.justchill.core.domain.report.GetMonthlyComparisonUseCase
import com.emm.justchill.core.domain.report.GetMonthlySectionStatsUseCase
import com.emm.justchill.core.domain.report.GetSavingsRateUseCase
import com.emm.justchill.core.domain.report.GetTopCategoriesOverMonthsUseCase
import com.emm.justchill.core.domain.report.MonthlyComparison
import com.emm.justchill.core.domain.report.MonthlySectionStats
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.domain.time.TodayFlow
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.presentation.error.toUserMessage
import com.emm.justchill.core.presentation.format.monthAbbrevLabel
import com.emm.justchill.core.presentation.format.monthLabel
import com.emm.justchill.core.presentation.mvi.MviViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlin.math.abs

const val TRENDS_WINDOW_MONTHS = 6
private const val MONTHS_FOR_A_MEANINGFUL_TREND = 3
private const val TOP_EXPENSES_SHOWN = 3

private fun barFraction(cents: Long, tallestCents: Long): Float =
    if (tallestCents == 0L) 0f else cents.toFloat() / tallestCents.toFloat()

class ReportViewModel(
    private val getMonthlyAmountByCategory: GetMonthlyAmountByCategoryUseCase,
    private val getMonthlyComparison: GetMonthlyComparisonUseCase,
    private val getMonthlySectionStats: GetMonthlySectionStatsUseCase,
    private val getSavingsRate: GetSavingsRateUseCase,
    private val getTopCategories: GetTopCategoriesOverMonthsUseCase,
    todayFlow: TodayFlow,
) : MviViewModel<ReportUiState, ReportIntent, ReportEffect>(
    ReportUiState(
        month = YearMonth.of(todayFlow.today()),
        isCurrentMonth = true,
        selectedType = TransactionType.Spend,
    ),
) {

    private val calendarMonth: StateFlow<YearMonth> = todayFlow()
        .map { date -> YearMonth.of(date) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, YearMonth.of(todayFlow.today()))

    private var reportJob: Job? = null
    private var trendsJob: Job? = null

    init {
        reloadReport()
        // The seed emission is the initial trends load, so init never calls reloadTrends() itself;
        // this updateState is the only isCurrentMonth correction that survives a failed trends load.
        calendarMonth
            .onEach {
                updateState { copy(isCurrentMonth = isCurrent(month)) }
                reloadTrends()
            }
            .launchSafeIn(onError = { e -> ReportEffect.ShowError(e.toUserMessage()) })
    }

    override fun onIntent(intent: ReportIntent) = when (intent) {
        ReportIntent.PreviousMonth -> showMonth(currentState.month.previous())
        ReportIntent.NextMonth -> showMonth(currentState.month.next())
        ReportIntent.JumpToCurrent -> showMonth(calendarMonth.value)
        is ReportIntent.SelectMonth -> showMonth(intent.month)
        is ReportIntent.SelectType -> onSelectType(intent.type)
        is ReportIntent.SelectTab -> onSelectTab(intent.tab)
        ReportIntent.ShareReport -> buildAndShareReport()
        ReportIntent.OnMonthSheetRequested -> updateState { copy(showMonthSheet = true) }
        ReportIntent.OnMonthSheetDismissed -> updateState { copy(showMonthSheet = false) }
    }

    private fun onSelectType(type: TransactionType) {
        updateState { copy(selectedType = type) }
        reloadReport()
    }

    private fun onSelectTab(tab: ReportTab) {
        updateState { copy(selectedTab = tab) }
        when (tab) {
            ReportTab.Month -> reloadReport()
            ReportTab.Trends -> reloadTrends()
        }
    }

    private fun showMonth(month: YearMonth) {
        updateState { copy(month = month, isCurrentMonth = isCurrent(month)) }
        reloadReport()
    }

    private fun isCurrent(month: YearMonth): Boolean = month == calendarMonth.value

    private fun reloadReport() {
        reportJob?.cancel()
        reportJob = launchSafe(onError = { e -> ReportEffect.ShowError(e.toUserMessage()) }) {
            val month: YearMonth = currentState.month
            val type: TransactionType = currentState.selectedType

            val incomeAmounts: List<CategoryAmount> = getMonthlyAmountByCategory(month, TransactionType.Income)
            val spendAmounts: List<CategoryAmount> = getMonthlyAmountByCategory(month, TransactionType.Spend)
            val isMonthEmpty: Boolean = incomeAmounts.isEmpty() && spendAmounts.isEmpty()

            val amounts: List<CategoryAmount> = if (type == TransactionType.Income) incomeAmounts else spendAmounts
            val comparison: MonthlyComparison? = getMonthlyComparison(month, type)
            val stats: MonthlySectionStats = getMonthlySectionStats(month, type)

            val total: Money = amounts.fold(Money.Zero) { acc, item -> acc + item.amount }

            val comparisonText: String? = comparison?.let { "vs ${month.previous().monthLabel()}" }
            val deltaCents: Long? = comparison?.absoluteDelta?.cents
            val comparisonAmountFormatted: String? = deltaCents?.let { cents -> formatSoles(abs(cents)) }
            val directionUp: Boolean? = deltaCents?.takeIf { cents -> cents != 0L }?.let { cents -> cents > 0 }
            val isPositive: Boolean? = directionUp?.let { up -> up == (type == TransactionType.Income) }

            val shares: List<CategoryShare> = buildShares(amounts, total)

            updateState {
                copy(
                    isCurrentMonth = isCurrent(month),
                    totalFormatted = formatSolesWithDecimals(total.cents),
                    comparisonText = comparisonText,
                    comparisonDirectionUp = directionUp,
                    comparisonIsPositive = isPositive,
                    comparisonAmountFormatted = comparisonAmountFormatted,
                    comparisonPercent = comparison?.let { abs(it.deltaPercent) } ?: 0,
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
            val currentYm = calendarMonth.value

            val savingsRate = getSavingsRate(currentYm, months = TRENDS_WINDOW_MONTHS)
            val topExpenses = getTopCategories(
                TransactionType.Spend,
                currentYm,
                months = TRENDS_WINDOW_MONTHS,
                topN = TOP_EXPENSES_SHOWN,
            )

            val isEarlyState = savingsRate.monthsWithData < MONTHS_FOR_A_MEANINGFUL_TREND

            val deltaText = savingsRate.deltaPointsVsPrior?.let { delta -> "${abs(delta)} pts" }
            val deltaIsPositive = savingsRate.deltaPointsVsPrior?.let { it >= 0 }

            val contextSentence = ReportShareFormatter.buildContextSentence(
                ratePercent = savingsRate.currentRatePercent,
                deltaPoints = savingsRate.deltaPointsVsPrior,
            )

            val tallestBarCents: Long = savingsRate.monthly
                .maxOfOrNull { total -> maxOf(total.income.cents, total.expense.cents) }
                ?: 0L
            val barItems = savingsRate.monthly.map { m ->
                MonthlyBarItem(
                    monthShortLabel = m.yearMonth.monthAbbrevLabel(),
                    isCurrentMonth = m.yearMonth == currentYm,
                    incomeAmount = m.income.cents,
                    expenseAmount = m.expense.cents,
                    incomeFormatted = formatSoles(m.income.cents),
                    expenseFormatted = formatSoles(m.expense.cents),
                    incomeFraction = barFraction(m.income.cents, tallestBarCents),
                    expenseFraction = barFraction(m.expense.cents, tallestBarCents),
                )
            }

            val topItems = topExpenses.map { it.toTopCategoryItem() }

            updateState {
                // The captured currentYm, not isCurrent(): the bars above used it, and one pass must not answer twice.
                copy(
                    isCurrentMonth = month == currentYm,
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

    private fun buildAndShareReport() {
        val state = currentState
        val text = when (state.selectedTab) {
            ReportTab.Month -> ReportShareFormatter.buildMonthShareText(state)
            ReportTab.Trends -> ReportShareFormatter.buildTrendsShareText(state)
        }
        sendEffect(ReportEffect.ShareReport(text))
    }
}
