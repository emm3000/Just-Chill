package com.emm.justchill.hh.home

import androidx.lifecycle.viewModelScope
import com.emm.domain.home.GetHomeDataUseCase
import com.emm.domain.home.HomeData
import com.emm.domain.recurring.ConfirmRecurringMovementUseCase
import com.emm.domain.recurring.PendingRecurring
import com.emm.domain.recurring.SkipRecurringMovementUseCase
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.YearMonth
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.hh.recurring.PendingRecurringUi
import com.emm.justchill.hh.recurring.toPendingRecurringUi
import com.emm.justchill.hh.transaction.toUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class HomeViewModel(
    private val getHomeData: GetHomeDataUseCase,
    private val confirmRecurringMovement: ConfirmRecurringMovementUseCase,
    private val skipRecurringMovement: SkipRecurringMovementUseCase,
    private val clock: kotlin.time.Clock,
    private val zone: TimeZone,
) : MviViewModel<HomeUiState, HomeIntent, HomeEffect>() {

    // Declared before initialState on purpose: property initializers run in order, so the state can
    // only borrow the month from here if here already exists. One read, one source of truth.
    private val selectedMonth = MutableStateFlow(YearMonth.current(clock, zone))

    override val initialState = HomeUiState(month = selectedMonth.value)

    init {
        selectedMonth
            .flatMapLatest { month -> getHomeData(month) }
            .onEach { homeData -> updateState { mapToUiState(homeData) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.PreviousMonth -> selectedMonth.value = selectedMonth.value.previous()
            HomeIntent.NextMonth -> selectedMonth.value = selectedMonth.value.next()
            is HomeIntent.ConfirmRecurring -> onConfirmRecurring(intent)
            is HomeIntent.SkipRecurring -> onSkipRecurring(intent)
        }
    }

    private fun onConfirmRecurring(intent: HomeIntent.ConfirmRecurring) {
        launchSafe(onError = { e -> HomeEffect.ShowError(e.toUserMessage()) }) {
            confirmRecurringMovement(
                templateId = RecurringMovementId(intent.templateId),
                yearMonth = intent.period,
                callerAmount = intent.callerAmount,
            )
            sendEffect(HomeEffect.CloseConfirmSheet)
        }
    }

    private fun onSkipRecurring(intent: HomeIntent.SkipRecurring) {
        launchSafe(onError = { e -> HomeEffect.ShowError(e.toUserMessage()) }) {
            skipRecurringMovement(
                templateId = RecurringMovementId(intent.templateId),
                yearMonth = intent.period,
            )
            sendEffect(HomeEffect.CloseConfirmSheet)
        }
    }

    private fun HomeUiState.mapToUiState(data: HomeData): HomeUiState = copy(
        month = selectedMonth.value,
        lastTransactions = data.lastTransactions.toUi(today()),
        income = data.income,
        spend = data.spend,
        balance = data.balance,
        hasAnyTransaction = data.hasAnyTransaction,
        pendingRecurringMovements = data.pendingRecurringMovements.toPendingUi(),
    )

    /**
     * The reference date the row labels resolve Hoy/Ayer against, read once per mapping pass so
     * every row in one emission agrees — and so the branch runs off this ViewModel's injected
     * clock instead of an ambient one buried in the mapper. The zone is injected alongside it for
     * the same reason: a zone read from the environment is a zone no test can put a boundary on.
     */
    private fun today(): LocalDate = clock.now().toLocalDateTime(zone).date

    // Current month comes from the clock, not from the selected month: it is what marks a pending
    // item as catch-up, and browsing to March must not relabel March's own pending row. Same clock
    // AND same zone as [today] — "what month is it" and "what day is it" cannot answer for two
    // different places, and reading the zone ambiently here is how they used to be able to.
    private fun List<PendingRecurring>.toPendingUi(): List<PendingRecurringUi> {
        val currentMonth = YearMonth.current(clock, zone)
        return map { it.toPendingRecurringUi(currentMonth) }
    }
}
