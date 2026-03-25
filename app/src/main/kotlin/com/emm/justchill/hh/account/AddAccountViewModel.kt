package com.emm.justchill.hh.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.account.CreateAccountUseCase
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
        accountCreator(name = state.name)
    }
}
