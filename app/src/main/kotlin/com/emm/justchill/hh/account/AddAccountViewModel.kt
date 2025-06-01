package com.emm.justchill.hh.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.account.AccountCreator
import com.emm.domain.account.AccountUpsert
import com.emm.justchill.core.formatInputToDouble
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class AddAccountViewModel(private val accountCreator: AccountCreator) : ViewModel() {

    var state by mutableStateOf(AddAccountUiState())
        private set

    init {
        combine(
            flow = snapshotFlow { state.balance },
            flow2 = snapshotFlow { state.name },
            transform = ::checkFields
        ).launchIn(viewModelScope)
    }

    fun onAction(action: AddAccountAction) {
        when (action) {
            is AddAccountAction.OnAmountChange -> state = state.copy(balance = action.value)
            is AddAccountAction.OnNameChange -> state = state.copy(name = action.value)
            AddAccountAction.OnSave -> save()
        }
    }

    private fun checkFields(mount: TextFieldValue, name: String) {
        val isEnabled: Boolean = mount.text.isNotEmpty() && name.isNotEmpty()
        state = state.copy(isEnabled = isEnabled)
    }

    private fun save() = viewModelScope.launch {
        val accountUpsert = AccountUpsert(
            name = state.name,
            balance = state.balance.formatInputToDouble(),
        )
        accountCreator.create(accountUpsert)
    }
}