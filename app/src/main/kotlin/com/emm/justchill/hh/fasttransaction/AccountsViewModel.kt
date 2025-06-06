package com.emm.justchill.hh.fasttransaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.account.Account
import com.emm.domain.account.AccountCreator
import com.emm.domain.account.AccountRepository
import com.emm.justchill.core.formatInputToDouble
import com.emm.justchill.hh.account.AddAccountAction
import com.emm.justchill.hh.account.AddAccountUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountsViewModel(
    private val accountCreator: AccountCreator,
    accountRepository: AccountRepository,
) : ViewModel() {

    var state by mutableStateOf(AddAccountUiState())
        private set

    init {
        combine(
            flow = snapshotFlow { state.balance },
            flow2 = snapshotFlow { state.name },
            transform = ::checkFields
        ).launchIn(viewModelScope)
    }

    val accounts: StateFlow<List<Account>> = accountRepository.all()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
        accountCreator.create(
            name = state.name,
            balance = state.balance.formatInputToDouble(),
        )
        state = AddAccountUiState()
    }
}