package com.emm.justchill.hh.category

import androidx.lifecycle.viewModelScope
import com.emm.domain.category.CreateCategoryUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import kotlinx.coroutines.launch

class AddCategoryViewModel(
    private val categoryCreator: CreateCategoryUseCase,
) : MviViewModel<AddCategoryUiState, AddCategoryIntent, AddCategoryEffect>() {

    override val initialState = AddCategoryUiState()

    override fun onIntent(intent: AddCategoryIntent) {
        when (intent) {
            is AddCategoryIntent.OnNameChange -> updateState {
                val isEnabled = intent.value.isNotEmpty() && intent.value.length >= 4
                copy(name = intent.value, isAllFieldValidated = isEnabled)
            }
            is AddCategoryIntent.OnCategoryTypeChange -> updateState { copy(categoryType = intent.value) }
            is AddCategoryIntent.OnColorChange -> updateState { copy(color = intent.value) }
            is AddCategoryIntent.OnIconChange -> updateState { copy(icon = intent.value) }
            AddCategoryIntent.OnSave -> saveCategory()
        }
    }

    private fun saveCategory() = viewModelScope.launch {
        try {
            categoryCreator(
                name = currentState.name,
                icon = currentState.icon.id,
                color = currentState.color.id,
                categoryType = currentState.categoryType,
            )
            sendEffect(AddCategoryEffect.CategorySaved)
        } catch (e: DomainException) {
            sendEffect(AddCategoryEffect.ShowError(e.toUserMessage()))
        } catch (e: Exception) {
            sendEffect(AddCategoryEffect.ShowError(DomainException.Unknown(e).toUserMessage()))
        }
    }
}
