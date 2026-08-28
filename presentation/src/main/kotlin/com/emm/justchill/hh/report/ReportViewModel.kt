package com.emm.justchill.hh.report

import androidx.lifecycle.viewModelScope
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
import com.emm.justchill.core.time.TodayFlow
import com.emm.justchill.hh.shared.monthAbbrevLabel
import com.emm.justchill.hh.shared.monthLabel
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

class ReportViewModel(
    private val getMonthlyAmountByCategory: GetMonthlyAmountByCategoryUseCase,
    private val getMonthlyComparison: GetMonthlyComparisonUseCase,
    private val getMonthlySectionStats: GetMonthlySectionStatsUseCase,
    private val getSavingsRate: GetSavingsRateUseCase,
    private val getTopCategories: GetTopCategoriesOverMonthsUseCase,
    todayFlow: TodayFlow,
) : MviViewModel<ReportUiState, ReportIntent, ReportEffect>(
    // Same seed calendarMonth's stateIn below uses, read before that StateFlow's first collection
    // — so it always equals calendarMonth.value here, and the opening month is always "current".
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
        // The seed emission is also the initial trends load, which is why init does not call
        // reloadTrends() itself; and this updateState is the only isCurrentMonth correction that
        // survives a trends load that fails.
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

    /** The single place the shown month changes, so `month` and its flag always move together. */
    private fun showMonth(month: YearMonth) {
        updateState { copy(month = month, isCurrentMonth = isCurrent(month)) }
        reloadReport()
    }

    private fun isCurrent(month: YearMonth): Boolean = month == calendarMonth.value

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

            val comparisonText = comparison?.let { "vs ${month.previous().monthLabel()}" }
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
                    isCurrentMonth = isCurrent(month),
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

            val barItems = savingsRate.monthly.map { m ->
                MonthlyBarItem(
                    monthShortLabel = m.yearMonth.monthAbbrevLabel(),
                    isCurrentMonth = m.yearMonth == currentYm,
                    incomeAmount = m.income.cents,
                    expenseAmount = m.expense.cents,
                    incomeFormatted = formatSoles(m.income.cents),
                    expenseFormatted = formatSoles(m.expense.cents),
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
