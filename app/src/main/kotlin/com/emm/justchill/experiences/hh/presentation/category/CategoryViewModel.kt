package com.emm.justchill.experiences.hh.presentation.category

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.experiences.hh.domain.CategoryAdder
import com.emm.justchill.experiences.hh.domain.TransactionType
import kotlinx.coroutines.launch

class CategoryViewModel(private val categoryAdder: CategoryAdder) : ViewModel() {

    var name by mutableStateOf("")
        private set

    var transactionType by mutableStateOf(TransactionType.SPENT)
        private set

    fun updateName(value: String) {
        name = value
    }

    fun updateTransactionType(value: TransactionType) {
        transactionType = value
    }

    fun addCategory() = viewModelScope.launch {
        categoryAdder.add(name, transactionType.name)
    }
}