package com.emm.justchill.hh.recurring

import androidx.lifecycle.viewModelScope
import com.emm.domain.recurring.DeleteRecurringMovementUseCase
import com.emm.domain.recurring.GetAllRecurringMovementsUseCase
import com.emm.domain.shared.RecurringMovementId
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class RecurringMovementsViewModel(
    getAllRecurring: GetAllRecurringMovementsUseCase,
    private val deleteRecurring: DeleteRecurringMovementUseCase,
) : MviViewModel<RecurringMovementsUiState, RecurringMovementsIntent, RecurringMovementsEffect>() {

    override val initialState = RecurringMovementsUiState()

    init {
        getAllRecurring()
            .onEach { list -> updateState { copy(items = list.map { it.toRecurringMovementUi() }) } }
            .launchIn(viewModelScope)
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
