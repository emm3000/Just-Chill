package com.emm.justchill.hh.report

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
import com.emm.justchill.hh.shared.monthAbbrevLabel
import com.emm.justchill.hh.shared.monthLabel
import kotlinx.coroutines.Job
import kotlinx.datetime.TimeZone
import kotlin.math.abs
import kotlin.time.Clock

/** Window the Tendencias tab reads. Long enough to show a season, short enough to stay relevant. */
private const val TRENDS_WINDOW_MONTHS = 6

/** Under this many months holding movements the trend is noise, and the UI says so. */
private const val MONTHS_FOR_A_MEANINGFUL_TREND = 3

/** How many categories the "en qué se te va" card lists. */
private const val TOP_EXPENSES_SHOWN = 3

@Suppress("LongParameterList")
class ReportViewModel(
    private val getMonthlyAmountByCategory: GetMonthlyAmountByCategoryUseCase,
    private val getMonthlyComparison: GetMonthlyComparisonUseCase,
    private val getMonthlySectionStats: GetMonthlySectionStatsUseCase,
    private val getSavingsRate: GetSavingsRateUseCase,
    private val getTopCategories: GetTopCategoriesOverMonthsUseCase,
    // Neither has a default, for the reason the two report use cases have none: an ambient default
    // is a silent read of the machine. Koin's constructor DSL ignores Kotlin defaults anyway
    // (see SharedModule), so a default here would only ever serve a hand-built instance.
    // "Which month is it" is "what day is it" asked at a coarser grain, and a zone read off the
    // device is one no test can move onto a month boundary — so nothing could prove which month
    // Reporte opens on for a user sitting in another zone.
    private val clock: Clock,
    private val zone: TimeZone,
) : MviViewModel<ReportUiState, ReportIntent, ReportEffect>() {

    override val initialState: ReportUiState = run {
        val opening = YearMonth.current(clock, zone)
        ReportUiState(
            month = opening,
            // A second read of the same clock, not the same read — [isCurrent] asks again, and
            // nothing here stops the two landing either side of midnight. Deriving it beats
            // hardcoding `true`, which would be correct only by coincidence of the line above; and
            // if the two reads ever did disagree the flag comes out false, which is the harmless
            // direction: a spurious TodayPill offers a jump that is already a no-op, where a
            // spurious `true` would hide the only one-tap way back.
            isCurrentMonth = isCurrent(opening),
            selectedType = TransactionType.Income,
        )
    }

    // Latest-wins jobs: cancels the in-flight load before starting the new one.
    private var reportJob: Job? = null
    private var trendsJob: Job? = null

    init {
        reloadReport()
        reloadTrends()
    }

    // Pure dispatch — exhaustiveness enforced by compiler via expression form. The four month
    // intents differ only in which month they ask for, so they go straight to [showMonth] rather
    // than through a handler each that would forward one expression.
    override fun onIntent(intent: ReportIntent) = when (intent) {
        ReportIntent.PreviousMonth -> showMonth(currentState.month.previous())
        ReportIntent.NextMonth -> showMonth(currentState.month.next())
        ReportIntent.JumpToCurrent -> showMonth(YearMonth.current(clock, zone))
        is ReportIntent.SelectMonth -> showMonth(intent.month)
        is ReportIntent.SelectType -> onSelectType(intent.type)
        is ReportIntent.SelectTab -> onSelectTab(intent.tab)
        ReportIntent.ShareReport -> buildAndShareReport()
    }

    // ── Intent handlers ───────────────────────────────────────────────────

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

    /** The single place the shown month changes, so `month` and its flag always move together. */
    private fun showMonth(month: YearMonth) {
        updateState { copy(month = month, isCurrentMonth = isCurrent(month)) }
        reloadReport()
    }

    /**
     * Whether [month] is the one the user is living in right now, off the injected clock and zone.
     *
     * Called from every path that writes state, not only from a month move. The check this replaced
     * lived in Compose, where recomposition re-evaluated it for free; a flag stored in state has to
     * be re-derived deliberately or it goes stale — a session left open across midnight on the 1st
     * would keep calling last month the current one, and `TodayPill`, the only one-tap way back,
     * would stay suppressed.
     *
     * The honest limit: "every path that writes state" is not "continuously". A screen sitting idle
     * with nothing loading does not notice a rollover until the next intent arrives. Reporte has no
     * resume hook to hang a refresh on, and giving it one is a separate change.
     */
    private fun isCurrent(month: YearMonth): Boolean = month == YearMonth.current(clock, zone)

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
                    // Re-read, not carried over: this load may be the first thing to happen after a
                    // month rolled over under an open screen.
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
            val savingsRate = getSavingsRate(clock = clock, zone = zone, months = TRENDS_WINDOW_MONTHS)
            val topExpenses = getTopCategories(
                TransactionType.Spend,
                clock = clock,
                zone = zone,
                months = TRENDS_WINDOW_MONTHS,
                topN = TOP_EXPENSES_SHOWN,
            )

            // Read per load, not per ViewModel: this may be the first thing to run after a month
            // rolled over under an open screen. Feeds both the bars and the state flag below.
            val currentYm = YearMonth.current(clock, zone)
            val isEarlyState = savingsRate.monthsWithData < MONTHS_FOR_A_MEANINGFUL_TREND

            // Magnitude only. Direction is `deltaIsPositive`, which the pill turns into a leading
            // arrow icon — spelling it out here too rendered "↓ ↓ 10 pts".
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
                copy(
                    // Same read the bars above used, so one pass cannot answer "is this the current
                    // month" two different ways.
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

    // ── Share ─────────────────────────────────────────────────────────────

    private fun buildAndShareReport() {
        val state = currentState
        val text = when (state.selectedTab) {
            ReportTab.Mes -> ReportShareFormatter.buildMesShareText(state)
            ReportTab.Tendencias -> ReportShareFormatter.buildTrendsShareText(state)
        }
        sendEffect(ReportEffect.ShareReport(text))
    }
}
