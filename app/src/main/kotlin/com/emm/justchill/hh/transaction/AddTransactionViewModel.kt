package com.emm.justchill.hh.transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.account.AccountRepository
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.domain.transaction.CreateTransactionUseCase
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.formatInputToDouble
import com.emm.justchill.hh.category.AppIconCatalog
import com.emm.justchill.hh.category.findById
import com.emm.justchill.hh.shared.Empty
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AddTransactionViewModel(
    private val transactionCreator: CreateTransactionUseCase,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private var dateInLong: Long = DateUtils.currentDateInMillis()

    var state by mutableStateOf(AddTransactionUiState())
        private set

    private val allCategories: MutableMap<CategoryType, List<SelectableCategory>> = mutableMapOf()

    init {
        combine(
            flow = snapshotFlow { state.amount },
            flow2 = snapshotFlow { state.date },
            flow3 = snapshotFlow { state.description },
            transform = ::validateFields,
        ).launchIn(viewModelScope)
        combine(
            flow = accountRepository.all(),
            flow2 = categoryRepository.all().map(::mapToUi),
        ) { accounts, categories ->
            allCategories.clear()
            val categoryMap = categories.groupBy(SelectableCategory::categoryType).toMutableMap()
            allCategories.putAll(categoryMap)
            state = state.copy(
                accounts = accounts,
                accountSelected = accounts.firstOrNull(),
                categories = allCategories[state.transactionType.categoryType]?.take(7).orEmpty(),
                categorySelected = allCategories[state.transactionType.categoryType]?.firstOrNull(),
            )
        }.launchIn(viewModelScope)
    }

    private fun validateFields(
        mount: TextFieldValue,
        date: String,
        description: String,
    ) {
        val isEnabled = mount.formatInputToDouble() >= 1.0
                && date.isNotEmpty()
                && description.isNotEmpty()
                && state.accountSelected != null
        state = state.copy(isEnabled = isEnabled)
    }

    fun onAction(action: AddTransactionAction) {
        when (action) {
            is AddTransactionAction.OnAmountChange -> state = state.copy(amount = action.value)
            is AddTransactionAction.OnDateChange -> state = state.copy(date = action.value)
            is AddTransactionAction.OnDescriptionChange -> state = state.copy(description = action.value)
            is AddTransactionAction.OnTransactionTypeChange -> {
                state = state.copy(
                    transactionType = action.value,
                    categories = allCategories[action.value.categoryType]?.take(7).orEmpty(),
                    categorySelected = allCategories[action.value.categoryType]?.firstOrNull(),
                )
            }
            is AddTransactionAction.OnDateChangeInMillis -> updateCurrentDate(action.value)
            AddTransactionAction.OnSave -> addTransaction()
            is AddTransactionAction.OnAccountSelected -> state = state.copy(accountSelected = action.value)
            is AddTransactionAction.OnCategorySelected -> state = state.copy(categorySelected = action.value)
            is AddTransactionAction.OnReset -> state = state.copy(
                amount = TextFieldValue("0.00"),
                description = String.Empty,
                date = DateUtils.currentDateAtReadableFormat(),
                transactionType = TransactionType.Income,
            )
            AddTransactionAction.OnDelete -> {}
            is AddTransactionAction.OnNewValueFromOthers -> {
                val updatedCategories = allCategories.values.flatten()
                    .filterNot { it.categoryId == action.value.categoryId }
                    .toMutableList()
                    .apply { add(0, action.value) }
                state = state.copy(
                    categories = updatedCategories.take(7),
                    categorySelected = action.value
                )
            }
        }
    }

    private fun addTransaction() = viewModelScope.launch {
        val transactionInsert: TransactionInsert = createTransactionInsert()
        transactionCreator(transactionInsert)
    }

    private fun createTransactionInsert() = TransactionInsert(
        type = state.transactionType,
        description = state.description,
        date = dateInLong,
        amount = state.amount.formatInputToDouble(),
        categoryId = state.categorySelected?.categoryId,
        accountId = state.accountSelected?.accountId ?: throw IllegalStateException(),
    )

    private fun updateCurrentDate(millis: Long?) = millis?.let {
        dateInLong = it
        state = state.copy(date = DateUtils.millisToReadableFormatUTC(it))
    }
}

private fun mapToUi(categories: List<Category>): List<SelectableCategory> = categories.map {
    SelectableCategory(
        categoryId = it.categoryId,
        name = it.name,
        icon = AppIconCatalog.findById(it.icon),
        categoryType = it.categoryType,
        color = findById(it.color)
    )
}
