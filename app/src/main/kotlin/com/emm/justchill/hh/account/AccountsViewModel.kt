package com.emm.justchill.hh.account

import androidx.lifecycle.viewModelScope
import com.emm.domain.account.AccountRepository
import com.emm.justchill.core.mvi.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AccountsViewModel(
    accountRepository: AccountRepository,
) : MviViewModel<AccountsUiState, AccountsIntent, AccountsEffect>(AccountsUiState()) {

    init {
        accountRepository.all()
            .onEach { accounts -> updateState { copy(accounts = accounts) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: AccountsIntent) = Unit
}
