package com.emm.justchill.hh.account

import androidx.lifecycle.viewModelScope
import com.emm.domain.account.CreateAccountUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import kotlinx.coroutines.launch

class AddAccountViewModel(
    private val accountCreator: CreateAccountUseCase,
) : MviViewModel<AddAccountUiState, AddAccountIntent, AddAccountEffect>(AddAccountUiState()) {

    override fun onIntent(intent: AddAccountIntent) {
        when (intent) {
            is AddAccountIntent.OnNameChange -> updateState {
                copy(name = intent.value, isEnabled = intent.value.isNotEmpty())
            }
            AddAccountIntent.OnSave -> save()
        }
    }

    private fun save() = viewModelScope.launch {
        try {
            accountCreator(name = currentState.name)
            sendEffect(AddAccountEffect.AccountSaved)
        } catch (e: DomainException) {
            sendEffect(AddAccountEffect.ShowError(e.toUserMessage()))
        } catch (e: Exception) {
            sendEffect(AddAccountEffect.ShowError(DomainException.Unknown(e).toUserMessage()))
        }
    }
}
