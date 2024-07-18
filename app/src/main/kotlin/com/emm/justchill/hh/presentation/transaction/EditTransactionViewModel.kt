package com.emm.justchill.hh.presentation.transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.justchill.Categories
import com.emm.justchill.Ga
import com.emm.justchill.hh.data.transaction.TransactionUpdate
import com.emm.justchill.hh.domain.TransactionType
import com.emm.justchill.hh.domain.category.CategoryLoader
import com.emm.justchill.hh.domain.transaction.TransactionFinder
import com.emm.justchill.hh.domain.transaction.TransactionUpdater
import com.emm.justchill.hh.domain.transaction.fromCentsToSoles
import com.emm.justchill.hh.domain.transactioncategory.AmountDbFormatter
import com.emm.justchill.hh.presentation.transaction.DateUtils.millisToReadableFormat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EditTransactionViewModel(
    categoryLoader: CategoryLoader,
    private val transactionId: String,
    private val transactionUpdater: TransactionUpdater,
    private val transactionFinder: TransactionFinder,
    private val amountCleaner: AmountDbFormatter,
) : ViewModel() {

    var amount by mutableStateOf("")
        private set

    var description by mutableStateOf("")
        private set

    var date by mutableStateOf(DateUtils.currentDateAtReadableFormat())
        private set

    private var dateInLong: Long = DateUtils.currentDateInMillis()

    var categoryLabel by mutableStateOf("")
        private set

    private var categoryId by mutableStateOf("")

    var isEnabled by mutableStateOf(false)
        private set

    var transactionType by mutableStateOf(TransactionType.INCOME)
        private set

    val categories: StateFlow<List<Categories>> = categoryLoader.load()
        .combine(snapshotFlow { transactionType }) { list, transactionType ->
            list.filter { it.type == transactionType.name }
        }.combine(snapshotFlow { categoryId }) { categories, categoryId ->
            categoryLabel = categories.firstOrNull { it.categoryId == categoryId }?.name.orEmpty()
            categories
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

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

        executeFind()
    }

    private fun executeFind() = viewModelScope.launch {
        val transaction: Ga = transactionFinder.find(transactionId) ?: return@launch
        amount = fromCentsToSoles(transaction.amount).toString()
        description = transaction.description
        transactionType = TransactionType.valueOf(transaction.type)
        date = millisToReadableFormat(transaction.date)
        categoryId = transaction.categoryId.orEmpty()
    }

    fun updateTransaction() = viewModelScope.launch {
        val transactionUpdate = TransactionUpdate(
            type = transactionType.name,
            description = description,
            date = dateInLong,
            amount = amountCleaner.format(amount)
        )
        transactionUpdater.update(transactionId, transactionUpdate, categoryId)
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
            date = millisToReadableFormat(millis)
        }
    }

    fun updateCategory(value: Categories) {
        categoryId = value.categoryId
    }

    fun updateTransactionType(value: TransactionType) {
        transactionType = value
    }

    fun updateCategoryLabel(value: String) {
        categoryLabel = value
    }
}