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

class CategoryViewModel(private val categoryCreator: CategoryCreator) : ViewModel() {

    var state by mutableStateOf(CategoryUiState())
        private set

    init {
        snapshotFlow { state.name }
            .onEach(::checkFields)
            .launchIn(viewModelScope)
    }

    fun onAction(action: CategoryAction) {
        when (action) {
            is CategoryAction.OnNameChange -> {
                state = state.copy(name = action.value)
            }

            is CategoryAction.OnCategoryTypeChange -> {
                state = state.copy(categoryType = action.value)
            }
            is CategoryAction.OnColorChange -> {
                state = state.copy(color = action.value)
            }
            is CategoryAction.OnIconChange -> {
                state = state.copy(icon = action.value)
            }
            CategoryAction.OnSave -> saveCategory()
        }
    }

    private fun checkFields(it: String) {
        val isEnabled: Boolean = it.isNotEmpty() && it.length >= 4
        state = state.copy(isAllFieldValidated = isEnabled)
    }

    private fun saveCategory() = viewModelScope.launch {
        categoryCreator.create(state.name)
    }
}