package com.emm.justchill.hh.presentation.category

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.hh.domain.categories.CategoryUpsert
import com.emm.justchill.hh.domain.categories.crud.CategoryCreator
import com.emm.justchill.hh.presentation.transaction.TransactionType
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class CategoryViewModel(private val categoryCreator: CategoryCreator) : ViewModel() {

    var name: String by mutableStateOf("")
        private set

    var description: String by mutableStateOf("")
        private set

    var transactionType by mutableStateOf(TransactionType.INCOME)
        private set

    var isEnabled by mutableStateOf(false)
        private set

    init {
        snapshotFlow { name }
            .onEach {
                isEnabled = name.isNotEmpty()
                        && name.length >= 4
            }.launchIn(viewModelScope)
    }

    fun updateName(value: String) {
        name = value
    }

    fun updateDescription(value: String) {
        description = value
    }

    fun updateTransactionType(value: TransactionType) {
        transactionType = value
    }

    fun save() = viewModelScope.launch {
        val categoryUpsert = CategoryUpsert(
            name = name,
            description = description,
            type = transactionType.name
        )
        categoryCreator.create(categoryUpsert)
    }
}