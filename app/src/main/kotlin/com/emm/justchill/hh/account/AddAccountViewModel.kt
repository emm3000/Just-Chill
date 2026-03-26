package com.emm.justchill.hh.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.account.CreateAccountUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.justchill.core.error.toUserMessage
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AddAccountViewModel(private val accountCreator: CreateAccountUseCase) : ViewModel() {

    var state by mutableStateOf(AddAccountUiState())
        private set

    init {
        snapshotFlow { state.name }
            .onEach { name ->
                state = state.copy(isEnabled = name.isNotEmpty())
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: AddAccountAction) {
        when (action) {
            is AddAccountAction.OnNameChange -> state = state.copy(name = action.value)
            AddAccountAction.OnSave -> save()
        }
    }

    private fun save() = viewModelScope.launch {
        try {
            accountCreator(name = state.name)
        } catch (e: DomainException) {
            state = state.copy(userMessage = e.toUserMessage())
        } catch (e: Exception) {
            state = state.copy(userMessage = DomainException.Unknown(e).toUserMessage())
        }
    }
}
