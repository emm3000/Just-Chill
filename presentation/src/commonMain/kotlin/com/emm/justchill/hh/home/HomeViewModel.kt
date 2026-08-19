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
    // only borrow the month from here if here already exists.
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

    private fun today(): LocalDate = clock.now().toLocalDateTime(zone).date

    /**
     * The current month comes from the clock, not the selected one: browsing to March must not
     * relabel March's own pending row.
     */
    private fun List<PendingRecurring>.toPendingUi(): List<PendingRecurringUi> {
        val currentMonth = YearMonth.current(clock, zone)
        return map { it.toPendingRecurringUi(currentMonth) }
    }
}
