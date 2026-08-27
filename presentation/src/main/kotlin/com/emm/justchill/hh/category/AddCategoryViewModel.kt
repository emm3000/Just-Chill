package com.emm.justchill.hh.category

import com.emm.domain.category.CategoryType
import com.emm.domain.category.CreateCategoryUseCase
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel

class AddCategoryViewModel(
    private val createCategory: CreateCategoryUseCase,
    initialType: CategoryType,
    initialName: String,
) : MviViewModel<AddCategoryUiState, AddCategoryIntent, AddCategoryEffect>(
    AddCategoryUiState(
        categoryType = initialType,
        name = initialName,
        isAllFieldValidated = initialName.isNotBlank(),
    ),
) {

    override fun onIntent(intent: AddCategoryIntent) {
        when (intent) {
            is AddCategoryIntent.OnNameChange -> updateState {
                copy(name = intent.value, isAllFieldValidated = intent.value.isNotBlank())
            }

            is AddCategoryIntent.OnCategoryTypeChange -> updateState { copy(categoryType = intent.value) }

            is AddCategoryIntent.OnColorChange -> updateState { copy(colorId = intent.value) }

            is AddCategoryIntent.OnIconChange -> updateState { copy(iconId = intent.value) }

            AddCategoryIntent.OnSave -> saveCategory()
        }
    }

    private fun saveCategory() = launchSafe(
        onError = { AddCategoryEffect.ShowError(it.toUserMessage()) },
    ) {
        val created = createCategory(
            name = currentState.name,
            icon = currentState.iconId,
            color = currentState.colorId,
            categoryType = currentState.categoryType,
        )
        sendEffect(AddCategoryEffect.CategorySaved(created))
    }
}
