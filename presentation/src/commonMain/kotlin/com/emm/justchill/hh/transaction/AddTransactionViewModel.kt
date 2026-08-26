package com.emm.justchill.hh.transaction

import androidx.lifecycle.viewModelScope
import com.emm.domain.account.AccountRepository
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.transaction.CreateTransactionUseCase
import com.emm.domain.transaction.GetFrequentCombosUseCase
import com.emm.domain.transaction.GetTopUsedCategoryIdsUseCase
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionStatsRepository
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Suppress("LongParameterList")
class AddTransactionViewModel(
    private val createTransaction: CreateTransactionUseCase,
    private val getTopUsedCategoryIds: GetTopUsedCategoryIdsUseCase,
    private val getFrequentCombos: GetFrequentCombosUseCase,
    private val transactionStatsRepository: TransactionStatsRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val clock: Clock,
    private val zone: TimeZone,
) : MviViewModel<AddTransactionUiState, AddTransactionIntent, AddTransactionEffect>() {

    override val initialState = AddTransactionUiState(today = today())

    init {
        viewModelScope.launch {
            val lastUsedAccountId = loadOrNull { transactionStatsRepository.lastUsedAccountId() }
            updateState { copy(lastUsedAccountId = lastUsedAccountId) }

            combine(
                flow = accountRepository.all(),
                flow2 = categoryRepository.all().map(::mapToUi),
            ) { accounts, categories ->
                Catalog.Loaded(accounts, categories.groupBy(SelectableCategory::categoryType))
            }
                .onEach { loaded -> updateState { copy(catalog = loaded) } }
                .launchIn(viewModelScope)
        }

        state.map { it.transactionType }
            .distinctUntilChanged()
            .flatMapLatest(::loadFrequentUsage)
            .onEach { usage -> updateState { copy(frequentUsage = usage) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: AddTransactionIntent) {
        // Every interaction re-reads the clock, so a screen left open overnight stops claiming that
        // yesterday is "Hoy". StateFlow drops the emission when the day has not changed, which is
        // every intent but the handful that cross midnight.
        updateState { copy(today = today()) }
        when (intent) {
            is AddTransactionIntent.OnAmountChange -> updateState { copy(amount = intent.value) }
            is AddTransactionIntent.OnDescriptionChange -> updateState { copy(description = intent.value) }
            is AddTransactionIntent.OnTransactionTypeChange -> updateState { copy(transactionType = intent.value) }
            is AddTransactionIntent.OnDateSelected -> updateState { copy(date = intent.value) }
            AddTransactionIntent.OnSave -> addTransaction()
            is AddTransactionIntent.OnAccountSelected -> updateState { copy(accountId = intent.value.accountId) }
            is AddTransactionIntent.OnCategorySelected -> updateState { copy(categoryId = intent.value.categoryId) }
            is AddTransactionIntent.OnFrequentComboSelected -> selectFrequentCombo(intent.value)
            is AddTransactionIntent.OnPreselectCombo -> registerPreselect(intent)
            is AddTransactionIntent.OnNewValueFromOthers -> addCategoryFromOthers(intent.value)
        }
    }

    private fun selectFrequentCombo(combo: FrequentComboUi) = updateState {
        copy(accountId = AccountId(combo.accountId), categoryId = CategoryId(combo.categoryId))
    }

    private fun registerPreselect(request: AddTransactionIntent.OnPreselectCombo) {
        if (currentState.preselectConsumed) return
        updateState {
            copy(
                preselectConsumed = true,
                transactionType = request.type ?: transactionType,
                accountId = request.accountId?.let(::AccountId) ?: accountId,
                categoryId = request.categoryId?.let(::CategoryId) ?: categoryId,
            )
        }
    }

    /**
     * The schema refuses a cross-type (categoryId, type) pair; the route always carries the
     * movement's type, so this guard should never fire.
     */
    private fun addCategoryFromOthers(category: SelectableCategory) {
        if (category.categoryType != currentState.transactionType.categoryType) return
        updateState {
            val others = extraCategories.filterNot { it.categoryId == category.categoryId }
            copy(extraCategories = listOf(category) + others, categoryId = category.categoryId)
        }
    }

    private fun loadFrequentUsage(type: TransactionType): Flow<FrequentUsage> = flow {
        val categoryIds = loadOrNull { getTopUsedCategoryIds(type) }.orEmpty().map { it.value }
        emit(FrequentUsage(loadedFor = type, categoryIds = categoryIds))

        val combos = loadOrNull { getFrequentCombos(type) }.orEmpty()
        emit(FrequentUsage(loadedFor = type, categoryIds = categoryIds, combos = combos))
    }

    private fun addTransaction() {
        if (currentState.isSaving) return
        updateState { copy(isSaving = true) }
        launchSafe(
            onError = {
                updateState { copy(isSaving = false) }
                AddTransactionEffect.ShowError(it.toUserMessage())
            },
        ) {
            val insert = currentState.toInsert(clock.now().toLocalDateTime(zone))
            createTransaction(insert)
            // Left true on purpose: the screen pops on this effect, and lowering it here would
            // re-enable the CTA during the navigation frame.
            sendEffect(AddTransactionEffect.TransactionSaved)
        }
    }

    private fun today(): LocalDate = clock.now().toLocalDateTime(zone).date
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

// The day is the user's, the hour is the moment of the save — both decided here, not from
// the state's cached `today`.
private fun AddTransactionUiState.toInsert(now: LocalDateTime): TransactionInsert = TransactionInsert(
    type = transactionType,
    description = description,
    occurredAt = LocalDateTime(date ?: now.date, now.time),
    amount = centsToMoney(amount),
    categoryId = categorySelected?.categoryId,
    accountId = accountSelected?.accountId
        ?: error("accountSelected required to build TransactionInsert — UI should have disabled save"),
)
