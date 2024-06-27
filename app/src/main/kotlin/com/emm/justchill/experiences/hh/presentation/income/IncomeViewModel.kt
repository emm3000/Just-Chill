package com.emm.justchill.experiences.hh.presentation.income

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.Categories
import com.emm.justchill.core.Result
import com.emm.justchill.experiences.hh.data.transaction.TransactionInsert
import com.emm.justchill.experiences.hh.domain.category.CategoryLoader
import com.emm.justchill.experiences.hh.domain.transactioncategory.TransactionCategoryAdder
import com.emm.justchill.experiences.hh.domain.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

class IncomeViewModel(
    categoryLoader: CategoryLoader,
    private val transactionCategoryAdder: TransactionCategoryAdder,
) : ViewModel() {

    var mount by mutableStateOf("")
        private set

    var description by mutableStateOf("")
        private set

    var date by mutableStateOf("")
        private set

    private var categoryId by mutableStateOf("")

    var isEnabled by mutableStateOf(false)
        private set


    init {
        combine(
            snapshotFlow { mount },
            snapshotFlow { date },
            snapshotFlow { categoryId },
        ) { mount, date, category ->
            isEnabled = mount.isNotEmpty()
                    && date.isNotEmpty()
                    && category.isNotEmpty()
        }.launchIn(viewModelScope)
    }

    val categories: StateFlow<Result<List<Categories>>> = categoryLoader.load()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = Result.Loading
        )

    fun addTransaction() = viewModelScope.launch {
        val transactionInsert = TransactionInsert(
            type = TransactionType.INCOME.name,
            mount = mount,
            description = description,
            date = Instant.now().toEpochMilli()
        )
        transactionCategoryAdder.add(categoryId, transactionInsert)
    }

    fun updateMount(value: String) {
        mount = value
    }

    fun updateDescription(value: String) {
        description = value
    }

    fun updateDate(value: String) {
        date = value
    }

    fun updateCategory(value: Categories) {
        categoryId = value.categoryId
    }
}