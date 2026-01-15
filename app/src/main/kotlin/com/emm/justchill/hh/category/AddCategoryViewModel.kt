package com.emm.justchill.hh.category

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.category.CategoryCreator
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AddCategoryViewModel(
    private val categoryCreator: CategoryCreator,
) : ViewModel() {

    var state by mutableStateOf(AddCategoryUiState())
        private set

    init {
        snapshotFlow { state.name }
            .onEach(::checkFields)
            .launchIn(viewModelScope)
    }

    fun onAction(action: AddCategoryAction) {
        when (action) {
            is AddCategoryAction.OnNameChange -> {
                state = state.copy(name = action.value)
            }

            is AddCategoryAction.OnCategoryTypeChange -> {
                state = state.copy(categoryType = action.value)
            }
            is AddCategoryAction.OnColorChange -> {
                state = state.copy(color = action.value)
            }
            is AddCategoryAction.OnIconChange -> {
                state = state.copy(icon = action.value)
            }
            AddCategoryAction.OnSave -> saveCategory()
        }
    }

    private fun checkFields(it: String) {
        val isEnabled: Boolean = it.isNotEmpty() && it.length >= 4
        state = state.copy(isAllFieldValidated = isEnabled)
    }

    private fun saveCategory() = viewModelScope.launch {
        categoryCreator.create(
            name = state.name,
            icon = state.icon.id,
            color = state.color.id,
            categoryType = state.categoryType
        )
    }
}