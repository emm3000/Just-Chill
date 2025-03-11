package com.emm.justchill.hh.category

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.category.CategoryUpsert
import com.emm.domain.category.CategoryCreator
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class CategoryViewModel(private val categoryCreator: CategoryCreator) : ViewModel() {

    var categoryUiState by mutableStateOf(CategoryUiState())
        private set

    init {
        snapshotFlow { categoryUiState.name }
            .onEach(::checkFields)
            .launchIn(viewModelScope)
    }

    fun onAction(action: CategoryAction) {
        when (action) {
            is CategoryAction.OnDescriptionChange -> {
                categoryUiState = categoryUiState.copy(description = action.value)
            }

            is CategoryAction.OnNameChange -> {
                categoryUiState = categoryUiState.copy(name = action.value)
            }

            is CategoryAction.OnTransactionTypeChange -> {
                categoryUiState = categoryUiState.copy(transactionType = action.value)
            }

            CategoryAction.OnSave -> saveCategory()
        }
    }

    private fun checkFields(it: String) {
        val isEnabled: Boolean = it.isNotEmpty() && it.length >= 4
        categoryUiState = categoryUiState.copy(isAllFieldValidated = isEnabled)
    }

    private fun saveCategory() = viewModelScope.launch {
        val categoryUpsert = CategoryUpsert(
            name = categoryUiState.name,
            description = categoryUiState.description,
            type = categoryUiState.transactionType
        )
        categoryCreator.create(categoryUpsert)
    }
}