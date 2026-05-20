package com.emm.justchill.hh.account

import androidx.lifecycle.viewModelScope
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.AccountUpsert
import com.emm.domain.account.DeleteAccountUseCase
import com.emm.domain.account.UpdateAccountUseCase
import com.emm.domain.transaction.TransactionRepository
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AccountsViewModel(
    accountRepository: AccountRepository,
    transactionRepository: TransactionRepository,
    private val updateAccount: UpdateAccountUseCase,
    private val deleteAccount: DeleteAccountUseCase,
) : MviViewModel<AccountsUiState, AccountsIntent, AccountsEffect>() {

    override val initialState = AccountsUiState()

    init {
        combine(
            accountRepository.all(),
            transactionRepository.all(),
        ) { accounts, transactions ->
            val counts = transactions.groupingBy { it.accountId }.eachCount()
            accounts to counts
        }
            .onEach { (accounts, counts) ->
                updateState { copy(accounts = accounts, movementCounts = counts) }
            }
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
