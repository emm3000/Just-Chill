package com.emm.justchill.hh.presentation.transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.Categories
import com.emm.justchill.core.Result
import com.emm.justchill.hh.domain.TransactionType
import com.emm.justchill.hh.domain.category.CategoryLoader
import com.emm.justchill.hh.domain.transactioncategory.TransactionCategoryAdder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionViewModel(
    categoryLoader: CategoryLoader,
    private val transactionCategoryAdder: TransactionCategoryAdder,
) : ViewModel() {

    var amount by mutableStateOf("")
        private set

    var description by mutableStateOf("")
        private set

    var date by mutableStateOf(DateUtils.currentDateAtReadableFormat())
        private set

    private var dateInLong: Long = DateUtils.currentDateInMillis()

    private var categoryId by mutableStateOf("")

    var isEnabled by mutableStateOf(false)
        private set

    var transactionType by mutableStateOf(TransactionType.INCOME)
        private set

    init {
        combine(
            snapshotFlow { amount },
            snapshotFlow { date },
            snapshotFlow { description },
            snapshotFlow { categoryId },
        ) { mount, date, description, category ->
            isEnabled = mount.isNotEmpty()
                    && date.isNotEmpty()
                    && description.isNotEmpty()
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
        val transactionInsert = com.emm.justchill.hh.data.transaction.TransactionInsert(
            type = transactionType.name,
            description = description,
            date = dateInLong
        )
        transactionCategoryAdder.add(categoryId, amount, transactionInsert)
    }

    fun updateMount(value: String) {
        amount = value
    }

    fun updateDescription(value: String) {
        description = value
    }

    fun updateCurrentDate(millis: Long?) {
        if (millis != null) {
            dateInLong = millis
            date = DateUtils.millisToReadableFormat(millis)
        }
    }

    fun updateCategory(value: Categories) {
        categoryId = value.categoryId
    }

    fun updateTransactionType(value: TransactionType) {
        transactionType = value
    }
}