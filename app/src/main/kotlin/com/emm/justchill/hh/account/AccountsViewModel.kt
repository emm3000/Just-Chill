package com.emm.justchill.hh.account

import androidx.lifecycle.viewModelScope
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.AccountUpsert
import com.emm.domain.account.DeleteAccountUseCase
import com.emm.domain.account.UpdateAccountUseCase
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AccountsViewModel(
    accountRepository: AccountRepository,
    private val updateAccount: UpdateAccountUseCase,
    private val deleteAccount: DeleteAccountUseCase,
) : MviViewModel<AccountsUiState, AccountsIntent, AccountsEffect>() {

    override val initialState = AccountsUiState()

    init {
        accountRepository.all()
            .onEach { accounts -> updateState { copy(accounts = accounts) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: AccountsIntent) {
        when (intent) {
            is AccountsIntent.OnEditClick -> updateState {
                copy(pendingEdit = intent.account, editName = intent.account.name)
            }
            is AccountsIntent.OnEditNameChange -> updateState { copy(editName = intent.value) }
            AccountsIntent.OnEditDismiss -> updateState { copy(pendingEdit = null, editName = "") }
            AccountsIntent.OnEditConfirm -> confirmEdit()

            is AccountsIntent.OnDeleteClick -> updateState { copy(pendingDelete = intent.account) }
            AccountsIntent.OnDeleteDismiss -> updateState { copy(pendingDelete = null) }
            AccountsIntent.OnDeleteConfirm -> confirmDelete()
        }
    }

    private fun confirmEdit() = launchSafe(
        onError = { e -> AccountsEffect.ShowMessage(e.toUserMessage()) },
    ) {
        val target = currentState.pendingEdit ?: return@launchSafe
        val newName = currentState.editName
        updateAccount(
            accountId = target.accountId,
            account = AccountUpsert(
                accountId = target.accountId,
                name = newName,
                type = target.type,
                currency = target.currency,
            ),
        )
        updateState { copy(pendingEdit = null, editName = "") }
    }

    private fun confirmDelete() = launchSafe(
        onError = { e ->
            updateState { copy(pendingDelete = null) }
            AccountsEffect.ShowMessage(e.toUserMessage())
        },
    ) {
        val target = currentState.pendingDelete ?: return@launchSafe
        deleteAccount(target.accountId)
        updateState { copy(pendingDelete = null) }
    }
}
