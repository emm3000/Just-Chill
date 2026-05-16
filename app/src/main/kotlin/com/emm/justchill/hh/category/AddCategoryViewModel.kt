package com.emm.justchill.hh.category

import com.emm.domain.category.CreateCategoryUseCase
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel

class AddCategoryViewModel(
    private val createCategory: CreateCategoryUseCase,
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

    private fun saveCategory() = launchSafe(
        onError = { AddCategoryEffect.ShowError(it.toUserMessage()) },
    ) {
        createCategory(
            name = currentState.name,
            icon = currentState.icon.id,
            color = currentState.color.id,
            categoryType = currentState.categoryType,
        )
        sendEffect(AddCategoryEffect.CategorySaved)
    }
}
