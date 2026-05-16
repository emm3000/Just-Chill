package com.emm.justchill.hh.account

import com.emm.domain.account.CreateAccountUseCase
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel

class AddAccountViewModel(
    private val createAccount: CreateAccountUseCase,
) : MviViewModel<AddAccountUiState, AddAccountIntent, AddAccountEffect>() {

    override val initialState = AddAccountUiState()

    override fun onIntent(intent: AddAccountIntent) {
        when (intent) {
            is AddAccountIntent.OnNameChange -> updateState {
                copy(name = intent.value, isEnabled = intent.value.isNotEmpty())
            }
            is AddAccountIntent.OnTypeChange -> updateState { copy(selectedType = intent.value) }
            is AddAccountIntent.OnCurrencyChange -> updateState { copy(selectedCurrency = intent.value) }
            AddAccountIntent.OnSave -> save()
        }
    }

    private fun save() = launchSafe(
        onError = { AddAccountEffect.ShowError(it.toUserMessage()) },
    ) {
        createAccount(
            name = currentState.name,
            type = currentState.selectedType,
            currency = currentState.selectedCurrency,
        )
        sendEffect(AddAccountEffect.AccountSaved)
    }
}
