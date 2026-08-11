package com.emm.justchill.hh.transaction

import androidx.lifecycle.viewModelScope
import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.AccountId
import com.emm.domain.transaction.CreateTransactionUseCase
import com.emm.domain.transaction.FrequentCombo
import com.emm.domain.transaction.GetFrequentCombosUseCase
import com.emm.domain.transaction.GetLastUsedAccountIdUseCase
import com.emm.domain.transaction.GetTopUsedCategoryIdsUseCase
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.hh.shared.Empty
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AddTransactionViewModel(
    private val createTransaction: CreateTransactionUseCase,
    private val getTopUsedCategoryIds: GetTopUsedCategoryIdsUseCase,
    private val getFrequentCombos: GetFrequentCombosUseCase,
    private val getLastUsedAccountId: GetLastUsedAccountIdUseCase,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
) : MviViewModel<AddTransactionUiState, AddTransactionIntent, AddTransactionEffect>() {

    override val initialState = AddTransactionUiState()

    private var dateInLong: Long = DateUtils.currentDateInMillis()
    private val allCategories: MutableMap<CategoryType, List<SelectableCategory>> = mutableMapOf()

    // Cached last-used account id — resolved before the combine flow fires.
    private var cachedLastUsedAccountId: AccountId? = null

    // Raw domain combos for the current type — resolved by loadFrequent, re-mapped when accounts/categories update.
    private var rawCombos: List<FrequentCombo> = emptyList()

    init {
        viewModelScope.launch {
            cachedLastUsedAccountId = runCatching { getLastUsedAccountId() }.getOrNull()

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
                        accountSelected = accountSelected ?: resolveLastUsedAccount(accounts),
                        categories = allCategories[transactionType.categoryType].orEmpty(),
                        categorySelected = categorySelected
                            ?: allCategories[transactionType.categoryType]?.firstOrNull(),
                        frequentCombos = buildComboUi(rawCombos, accounts, allCategories),
                    ).validate()
                }
            }.launchIn(viewModelScope)

            loadFrequent(currentState.transactionType)
        }
    }

    override fun onIntent(intent: AddTransactionIntent) {
        when (intent) {
            is AddTransactionIntent.OnAmountChange -> updateState { copy(amount = intent.value).touched() }

            is AddTransactionIntent.OnDateChange -> updateState { copy(date = intent.value).touched() }

            is AddTransactionIntent.OnDescriptionChange -> updateState { copy(description = intent.value).touched() }

            is AddTransactionIntent.OnTransactionTypeChange -> changeTransactionType(intent.value)

            is AddTransactionIntent.OnDateChangeInMillis -> updateCurrentDate(intent.value)

            AddTransactionIntent.OnSave -> addTransaction()

            is AddTransactionIntent.OnAccountSelected -> updateState { copy(accountSelected = intent.value).touched() }

            is AddTransactionIntent.OnCategorySelected -> updateState {
                copy(
                    categorySelected = intent.value,
                ).touched()
            }

            is AddTransactionIntent.OnFrequentComboSelected -> selectFrequentCombo(intent.value)

            AddTransactionIntent.OnReset -> reset()

            is AddTransactionIntent.OnNewValueFromOthers -> addCategoryFromOthers(intent.value)
        }
    }

    private fun changeTransactionType(type: TransactionType) {
        updateState {
            copy(
                transactionType = type,
                categories = allCategories[type.categoryType].orEmpty(),
                categorySelected = allCategories[type.categoryType]?.firstOrNull(),
            ).touched()
        }
        loadFrequent(type)
    }

    private fun selectFrequentCombo(combo: FrequentComboUi) {
        val account = currentState.accounts.find { it.accountId.value == combo.accountId }
        val category = allCategories.values.flatten()
            .find { it.categoryId.value == combo.categoryId }
        if (account == null || category == null) return
        updateState {
            copy(
                accountSelected = account,
                categorySelected = category,
            ).touched()
        }
        sendEffect(AddTransactionEffect.FocusAmountField)
    }

    private fun reset() {
        dateInLong = DateUtils.currentDateInMillis()
        updateState {
            val defaultType = TransactionType.Income
            copy(
                amount = "",
                description = String.Empty,
                date = DateUtils.friendlyDate(dateInLong),
                transactionType = defaultType,
                categories = allCategories[defaultType.categoryType].orEmpty(),
                categorySelected = allCategories[defaultType.categoryType]?.firstOrNull(),
                accountSelected = resolveLastUsedAccount(accounts),
                isEnabled = false,
                hasChanges = false,
            )
        }
    }

    private fun addCategoryFromOthers(category: SelectableCategory) {
        val updatedCategories = allCategories.values.flatten()
            .filterNot { it.categoryId == category.categoryId }
            .toMutableList()
            .apply { add(0, category) }
        updateState {
            copy(
                categories = updatedCategories,
                categorySelected = category,
            ).touched()
        }
    }

    private fun resolveLastUsedAccount(accounts: List<Account>): Account? {
        val lastId = cachedLastUsedAccountId
        return if (lastId != null) {
            accounts.find { it.accountId == lastId } ?: accounts.firstOrNull()
        } else {
            accounts.firstOrNull()
        }
    }

    private fun loadFrequent(type: TransactionType) = viewModelScope.launch {
        val ids = runCatching { getTopUsedCategoryIds(type) }.getOrDefault(emptyList())
        updateState { copy(frequentCategoryIds = ids.map { it.value }) }

        rawCombos = runCatching { getFrequentCombos(type) }.getOrDefault(emptyList())
        val comboUiList = buildComboUi(rawCombos, currentState.accounts, allCategories)
        updateState { copy(frequentCombos = comboUiList) }
    }

    private fun buildComboUi(
        combos: List<FrequentCombo>,
        accounts: List<Account>,
        categories: Map<CategoryType, List<SelectableCategory>>,
    ): List<FrequentComboUi> = combos.mapNotNull { combo ->
        val account = accounts.find { it.accountId == combo.accountId }
            ?: return@mapNotNull null
        val category = categories.values.flatten()
            .find { it.categoryId == combo.categoryId }
            ?: return@mapNotNull null
        FrequentComboUi(
            accountId = combo.accountId.value,
            categoryId = combo.categoryId.value,
            type = combo.type,
            label = "${account.name} · ${category.name}",
            colorId = category.colorId,
        )
    }

    private fun addTransaction() = launchSafe(
        onError = { AddTransactionEffect.ShowError(it.toUserMessage()) },
    ) {
        createTransaction(createTransactionInsert())
        cachedLastUsedAccountId = currentState.accountSelected?.accountId
        sendEffect(AddTransactionEffect.TransactionSaved)
    }

    private fun createTransactionInsert(): TransactionInsert = TransactionInsert(
        type = currentState.transactionType,
        description = currentState.description,
        date = dateInLong,
        amount = centsToMoney(currentState.amount),
        categoryId = currentState.categorySelected?.categoryId,
        accountId = currentState.accountSelected?.accountId
            ?: error("accountSelected required to build TransactionInsert — UI should have disabled save"),
    )

    private fun updateCurrentDate(millis: Long?) = millis?.let {
        dateInLong = it
        updateState { copy(date = DateUtils.friendlyDate(it)).touched() }
    }
}

private fun mapToUi(categories: List<Category>): List<SelectableCategory> = categories.map {
    SelectableCategory(
        categoryId = it.categoryId,
        name = it.name,
        iconId = it.icon,
        categoryType = it.categoryType,
        colorId = it.color,
    )
}

private fun AddTransactionUiState.validate(): AddTransactionUiState = copy(isEnabled = missingField == null)

private fun AddTransactionUiState.touched(): AddTransactionUiState = validate().copy(hasChanges = true)
