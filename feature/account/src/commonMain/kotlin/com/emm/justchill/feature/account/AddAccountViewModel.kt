package com.emm.justchill.feature.account

import com.emm.justchill.core.domain.account.CreateAccountUseCase
import com.emm.justchill.core.domain.shared.error.DomainException
import com.emm.justchill.core.domain.shared.error.ValidationCode
import com.emm.justchill.core.presentation.error.toUserMessage
import com.emm.justchill.core.presentation.mvi.MviViewModel

class AddAccountViewModel(private val createAccount: CreateAccountUseCase) :
    MviViewModel<AddAccountUiState, AddAccountIntent, AddAccountEffect>(AddAccountUiState()) {

    override fun onIntent(intent: AddAccountIntent) {
        when (intent) {
            is AddAccountIntent.OnNameChange -> updateState {
                copy(name = intent.value, isEnabled = intent.value.isNotEmpty(), nameError = null)
            }

            is AddAccountIntent.OnTypeChange -> updateState { copy(selectedType = intent.value) }

            AddAccountIntent.OnSave -> save()
        }
    }

    private fun save() = launchSafe(onError = ::refuse) {
        createAccount(
            name = currentState.name,
            type = currentState.selectedType,
        )
        sendEffect(AddAccountEffect.AccountSaved)
    }

    private fun refuse(error: DomainException): AddAccountEffect? {
        val code: ValidationCode? = (error as? DomainException.ValidationError)?.code
        if (code != ValidationCode.NameRequired) return AddAccountEffect.ShowError(error.toUserMessage())
        updateState { copy(nameError = error.toUserMessage()) }
        return null
    }
}
