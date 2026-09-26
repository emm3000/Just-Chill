package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.domain.account.AccountRepository
import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.category.CategoryRepository
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.domain.time.TodayFlow
import com.emm.justchill.core.domain.transaction.CreateTransactionUseCase
import com.emm.justchill.core.domain.transaction.FrequentCombo
import com.emm.justchill.core.domain.transaction.GetFrequentCombosUseCase
import com.emm.justchill.core.domain.transaction.GetMonthSpendUseCase
import com.emm.justchill.core.domain.transaction.GetTopUsedCategoryIdsUseCase
import com.emm.justchill.core.domain.transaction.TransactionInsert
import com.emm.justchill.core.domain.transaction.TransactionStatsRepository
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.category.SelectableCategory
import com.emm.justchill.core.ui.category.toSelectable
import com.emm.justchill.core.ui.error.toUserMessage
import com.emm.justchill.core.ui.format.centsToMoney
import com.emm.justchill.core.ui.mvi.MviViewModel
import com.emm.justchill.core.ui.transaction.Catalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

internal const val RERANK_DEBOUNCE_MILLIS: Long = 300L

@Suppress("LongParameterList")
class AddTransactionViewModel(
    private val createTransaction: CreateTransactionUseCase,
    private val getTopUsedCategoryIds: GetTopUsedCategoryIdsUseCase,
    private val getFrequentCombos: GetFrequentCombosUseCase,
    private val getMonthSpend: GetMonthSpendUseCase,
    private val transactionStatsRepository: TransactionStatsRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val todayFlow: TodayFlow,
    private val clock: Clock,
    private val zone: TimeZone,
) : MviViewModel<AddTransactionUiState, AddTransactionIntent, AddTransactionEffect>(
    AddTransactionUiState(today = todayFlow.today()),
) {

    init {
        // Nested on purpose: hoisting accountRepository.all() would read the catalog while the
        // last-used account is still pending, flashing accounts.first() before it lands.
        launchSafe(onError = { AddTransactionEffect.ShowError(it.toUserMessage()) }) {
            val lastUsedAccountId: AccountId? = loadOrNull { transactionStatsRepository.lastUsedAccountId() }
            updateState { copy(lastUsedAccountId = lastUsedAccountId) }

            combine(
                flow = accountRepository.all(),
                flow2 = categoryRepository.all().map { categories -> categories.map(Category::toSelectable) },
            ) { accounts, categories ->
                Catalog.Loaded(accounts, categories.groupBy(SelectableCategory::categoryType))
            }
                .onEach { loaded -> updateState { copy(catalog = loaded) } }
                .launchSafeIn(onError = { AddTransactionEffect.ShowError(it.toUserMessage()) })
        }

        combine(
            flow = state.map { it.transactionType }.distinctUntilChanged(),
            flow2 = typedAmounts(),
        ) { type, amount -> ComboQuery(type, amount) }
            .distinctUntilChanged()
            .flatMapLatest(::loadFrequentUsage)
            .onEach { usage -> updateState { copy(frequentUsage = usage) } }
            .launchSafeIn(onError = { AddTransactionEffect.ShowError(it.toUserMessage()) })

        todayFlow()
            .map(YearMonth::of)
            .distinctUntilChanged()
            .flatMapLatest { month -> getMonthSpend(month).map { total -> MonthSpend(month, total) } }
            .onEach { spend -> updateState { copy(monthSpend = spend) } }
            .launchSafeIn(onError = { AddTransactionEffect.ShowError(it.toUserMessage()) })
    }

    override fun onIntent(intent: AddTransactionIntent) {
        updateState { copy(today = todayFlow.today()) }
        when (intent) {
            is AddTransactionIntent.OnAmountChange -> updateState { copy(amount = intent.value) }
            is AddTransactionIntent.OnDescriptionChange -> updateState { copy(description = intent.value) }
            is AddTransactionIntent.OnTransactionTypeChange -> updateState { copy(transactionType = intent.value) }
            is AddTransactionIntent.OnDateSelected -> updateState { copy(date = intent.value) }
            AddTransactionIntent.OnSave -> addTransaction()
            is AddTransactionIntent.OnAccountSelected -> updateState { copy(accountId = intent.value.accountId) }
            is AddTransactionIntent.OnCategorySelected -> updateState { copy(categoryId = intent.value.categoryId) }
            is AddTransactionIntent.OnPreselectCombo -> registerPreselect(intent)
            is AddTransactionIntent.OnNewValueFromOthers -> addCategoryFromOthers(intent.value)
            is AddTransactionIntent.OnSheetRequested -> updateState { copy(openSheet = intent.sheet) }
            AddTransactionIntent.OnSheetDismissed -> updateState { copy(openSheet = null) }
        }
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

    private fun addCategoryFromOthers(category: SelectableCategory) {
        if (category.categoryType != currentState.transactionType.categoryType) return
        updateState {
            val others: List<SelectableCategory> = extraCategories.filterNot { it.categoryId == category.categoryId }
            copy(extraCategories = listOf(category) + others, categoryId = category.categoryId)
        }
    }

    private fun typedAmounts(): Flow<Money?> = state
        .map { centsToMoney(it.amount).takeIf { typed -> typed != Money.Zero } }
        .distinctUntilChanged()
        .debounce { amount -> if (amount == null) 0L else RERANK_DEBOUNCE_MILLIS }

    private fun loadFrequentUsage(query: ComboQuery): Flow<FrequentUsage> = flow {
        val type: TransactionType = query.type
        val categoryIds: List<String> = loadOrNull { getTopUsedCategoryIds(type) }.orEmpty().map { it.value }
        val settled: List<FrequentCombo> = currentState.frequentUsage
            ?.takeIf { it.loadedFor == type }
            ?.combos
            .orEmpty()
        emit(FrequentUsage(loadedFor = type, categoryIds = categoryIds, combos = settled))

        val combos: List<FrequentCombo> = loadOrNull { getFrequentCombos(type, amount = query.amount) }.orEmpty()
        emit(FrequentUsage(loadedFor = type, categoryIds = categoryIds, combos = combos))
    }

    private data class ComboQuery(val type: TransactionType, val amount: Money?)

    private fun addTransaction() {
        if (currentState.isSaving) return
        updateState { copy(isSaving = true) }
        launchSafe(
            onError = {
                updateState { copy(isSaving = false) }
                AddTransactionEffect.ShowError(it.toUserMessage())
            },
        ) {
            val timeOfDay: LocalTime = clock.now().toLocalDateTime(zone).time
            val insert: TransactionInsert = currentState.toInsert(day = todayFlow.today(), time = timeOfDay)
            createTransaction(insert)
            sendEffect(AddTransactionEffect.TransactionSaved)
        }
    }
}

private fun AddTransactionUiState.toInsert(day: LocalDate, time: LocalTime): TransactionInsert = TransactionInsert(
    type = transactionType,
    description = description,
    occurredAt = LocalDateTime(date ?: day, time),
    amount = centsToMoney(amount),
    categoryId = categorySelected?.categoryId,
    accountId = accountSelected?.accountId
        ?: error("accountSelected required to build TransactionInsert — UI should have disabled save"),
)
