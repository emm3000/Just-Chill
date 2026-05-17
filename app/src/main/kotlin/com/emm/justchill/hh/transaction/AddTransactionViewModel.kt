package com.emm.justchill.hh.transaction

import androidx.lifecycle.viewModelScope
import com.emm.domain.account.AccountRepository
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.domain.transaction.CreateTransactionUseCase
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.hh.category.AppIconCatalog
import com.emm.justchill.hh.category.findById
import com.emm.justchill.hh.shared.Empty
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map

class AddTransactionViewModel(
    private val createTransaction: CreateTransactionUseCase,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
) : MviViewModel<AddTransactionUiState, AddTransactionIntent, AddTransactionEffect>() {

    override val initialState = AddTransactionUiState()

    private var dateInLong: Long = DateUtils.currentDateInMillis()
    private val allCategories: MutableMap<CategoryType, List<SelectableCategory>> = mutableMapOf()

    init {
        combine(
            flow = accountRepository.all(),
            flow2 = categoryRepository.all().map(::mapToUi),
        ) { accounts, categories ->
            allCategories.clear()
            val categoryMap = categories.groupBy(SelectableCategory::categoryType).toMutableMap()
            allCategories.putAll(categoryMap)
            updateState {
                copy(
                    accounts = accounts,
                    accountSelected = accounts.firstOrNull(),
                    categories = allCategories[transactionType.categoryType]?.take(7).orEmpty(),
                    categorySelected = allCategories[transactionType.categoryType]?.firstOrNull(),
                )
            }
        }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: AddTransactionIntent) {
        when (intent) {
            is AddTransactionIntent.OnAmountChange -> updateState { copy(amount = intent.value).recomputeValidity() }
            is AddTransactionIntent.OnDateChange -> updateState { copy(date = intent.value).recomputeValidity() }
            is AddTransactionIntent.OnDescriptionChange -> updateState { copy(description = intent.value).recomputeValidity() }
            is AddTransactionIntent.OnTransactionTypeChange -> updateState {
                copy(
                    transactionType = intent.value,
                    categories = allCategories[intent.value.categoryType]?.take(7).orEmpty(),
                    categorySelected = allCategories[intent.value.categoryType]?.firstOrNull(),
                )
            }
            is AddTransactionIntent.OnDateChangeInMillis -> updateCurrentDate(intent.value)
            AddTransactionIntent.OnSave -> addTransaction()
            is AddTransactionIntent.OnAccountSelected -> updateState { copy(accountSelected = intent.value) }
            is AddTransactionIntent.OnCategorySelected -> updateState { copy(categorySelected = intent.value) }
            AddTransactionIntent.OnReset -> updateState {
                copy(
                    amount = "",
                    description = String.Empty,
                    date = DateUtils.currentDateAtReadableFormat(),
                    transactionType = TransactionType.Income,
                )
            }
            is AddTransactionIntent.OnNewValueFromOthers -> {
                val updatedCategories = allCategories.values.flatten()
                    .filterNot { it.categoryId == intent.value.categoryId }
                    .toMutableList()
                    .apply { add(0, intent.value) }
                updateState {
                    copy(
                        categories = updatedCategories.take(7),
                        categorySelected = intent.value,
                    )
                }
            }
        }
    }

    private fun AddTransactionUiState.recomputeValidity(): AddTransactionUiState =
        copy(isEnabled = centsToSoles(amount) >= 1.0 && date.isNotEmpty() && description.isNotEmpty() && accountSelected != null)

    private fun addTransaction() = launchSafe(
        onError = { AddTransactionEffect.ShowError(it.toUserMessage()) },
    ) {
        createTransaction(createTransactionInsert())
        sendEffect(AddTransactionEffect.TransactionSaved)
    }

    private fun createTransactionInsert(): TransactionInsert = TransactionInsert(
        type = currentState.transactionType,
        description = currentState.description,
        date = dateInLong,
        amount = centsToMoney(currentState.amount),
        categoryId = currentState.categorySelected?.categoryId,
        accountId = currentState.accountSelected?.accountId ?: throw IllegalStateException(),
    )

    private fun updateCurrentDate(millis: Long?) = millis?.let {
        dateInLong = it
        updateState { copy(date = DateUtils.millisToReadableFormatUTC(it)) }
    }
}

private fun mapToUi(categories: List<Category>): List<SelectableCategory> = categories.map {
    SelectableCategory(
        categoryId = it.categoryId,
        name = it.name,
        icon = AppIconCatalog.findById(it.icon),
        categoryType = it.categoryType,
        color = findById(it.color),
    )
}
