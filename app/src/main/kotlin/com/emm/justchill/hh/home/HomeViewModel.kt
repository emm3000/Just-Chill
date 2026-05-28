package com.emm.justchill.hh.home

import androidx.lifecycle.viewModelScope
import com.emm.domain.home.GetHomeDataUseCase
import com.emm.domain.home.HomeData
import com.emm.domain.recurring.ConfirmRecurringMovementUseCase
import com.emm.domain.recurring.RecurringMovement
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
    private val clock: kotlin.time.Clock = kotlin.time.Clock.System,
) : MviViewModel<HomeUiState, HomeIntent, HomeEffect>() {

    override val initialState = HomeUiState()

    private val selectedMonth = MutableStateFlow(YearMonth.current())

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
        }
    }

    private fun onConfirmRecurring(intent: HomeIntent.ConfirmRecurring) {
        launchSafe(onError = { e -> HomeEffect.ShowError(e.toUserMessage()) }) {
            val today: LocalDate = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            confirmRecurringMovement(
                templateId = RecurringMovementId(intent.templateId),
                yearMonth = selectedMonth.value,
                today = today,
                callerAmount = intent.callerAmount,
            )
            sendEffect(HomeEffect.CloseConfirmSheet)
        }
    }

    private fun HomeUiState.mapToUiState(data: HomeData): HomeUiState = copy(
        month = selectedMonth.value,
        lastTransactions = data.lastTransactions.toUi(),
        income = data.income,
        spend = data.spend,
        balance = data.balance,
        hasAnyTransaction = data.hasAnyTransaction,
        pendingRecurringMovements = data.pendingRecurringMovements.toPendingUi(),
    )

    private fun List<RecurringMovement>.toPendingUi(): List<PendingRecurringUi> = map { it.toPendingRecurringUi() }
}
