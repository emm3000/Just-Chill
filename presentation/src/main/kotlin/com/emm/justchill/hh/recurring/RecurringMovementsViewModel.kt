package com.emm.justchill.hh.recurring

import com.emm.justchill.core.domain.recurring.DeleteRecurringMovementUseCase
import com.emm.justchill.core.domain.recurring.GetRecurringMonthlyTotalsUseCase
import com.emm.justchill.core.domain.recurring.RecurringMovementRepository
import com.emm.justchill.core.domain.shared.RecurringMovementId
import com.emm.justchill.core.ui.error.toUserMessage
import com.emm.justchill.core.ui.format.format
import com.emm.justchill.core.ui.format.formatNeutral
import com.emm.justchill.core.ui.mvi.MviViewModel
import kotlinx.coroutines.flow.onEach

class RecurringMovementsViewModel(
    recurringMovementRepository: RecurringMovementRepository,
    private val getTotals: GetRecurringMonthlyTotalsUseCase,
    private val deleteRecurring: DeleteRecurringMovementUseCase,
) : MviViewModel<RecurringMovementsUiState, RecurringMovementsIntent, RecurringMovementsEffect>(
    RecurringMovementsUiState(),
) {

    init {
        recurringMovementRepository.allWithDetails()
            .onEach { list ->
                val activeItems = list
                    .filter { it.isActive }
                    .map { it.toRecurringMovementUi() }
                    .sortedWith(compareBy({ it.dayOfMonth }, { it.name }))
                val pausedItems = list
                    .filter { !it.isActive }
                    .map { it.toRecurringMovementUi() }
                    .sortedWith(compareBy({ it.dayOfMonth }, { it.name }))
                val totals = getTotals(list)
                val entranFormatted = formatNeutral(totals.incomeTotal.format())
                val salenFormatted = formatNeutral(totals.expenseTotal.format())
                updateState {
                    copy(
                        activeItems = activeItems,
                        pausedItems = pausedItems,
                        entranFormatted = entranFormatted,
                        salenFormatted = salenFormatted,
                        variableCount = totals.activeVariableCount,
                    )
                }
            }
            .launchSafeIn(onError = { e -> RecurringMovementsEffect.ShowError(e.toUserMessage()) })
    }

    override fun onIntent(intent: RecurringMovementsIntent) {
        when (intent) {
            RecurringMovementsIntent.NavigateToAdd -> sendEffect(RecurringMovementsEffect.NavigateToAddEdit())

            is RecurringMovementsIntent.NavigateToEdit -> {
                sendEffect(RecurringMovementsEffect.NavigateToAddEdit(intent.id))
            }

            is RecurringMovementsIntent.RequestDelete -> updateState { copy(pendingDelete = intent.id) }

            RecurringMovementsIntent.DismissDelete -> updateState { copy(pendingDelete = null) }

            RecurringMovementsIntent.ConfirmDelete -> confirmDelete()
        }
    }

    private fun confirmDelete() = launchSafe(
        onError = { e ->
            updateState { copy(pendingDelete = null) }
            RecurringMovementsEffect.ShowError(e.toUserMessage())
        },
    ) {
        val id = currentState.pendingDelete ?: return@launchSafe
        deleteRecurring(RecurringMovementId(id))
        updateState { copy(pendingDelete = null) }
    }
}
