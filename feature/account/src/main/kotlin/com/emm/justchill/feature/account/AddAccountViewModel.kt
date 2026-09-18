package com.emm.justchill.feature.account

import com.emm.justchill.core.domain.account.CreateAccountUseCase
import com.emm.justchill.core.ui.error.toUserMessage
import com.emm.justchill.core.ui.mvi.MviViewModel

class AddAccountViewModel(private val createAccount: CreateAccountUseCase) :
    MviViewModel<AddAccountUiState, AddAccountIntent, AddAccountEffect>(AddAccountUiState()) {

    override fun onIntent(intent: AddAccountIntent) {
        when (intent) {
            is AddAccountIntent.OnNameChange -> updateState {
                copy(name = intent.value, isEnabled = intent.value.isNotEmpty())
            }

            is AddAccountIntent.OnTypeChange -> updateState { copy(selectedType = intent.value) }

            AddAccountIntent.OnSave -> save()
        }
    }

    private fun save() = launchSafe(
        onError = { AddAccountEffect.ShowError(it.toUserMessage()) },
    ) {
        createAccount(
            name = currentState.name,
            type = currentState.selectedType,
        )
        sendEffect(AddAccountEffect.AccountSaved)
    }
}
