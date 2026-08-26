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
import com.emm.domain.transaction.GetTopUsedCategoryIdsUseCase
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionStatsRepository
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.hh.shared.comboLabel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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

    private val allCategories: MutableMap<CategoryType, List<SelectableCategory>> = mutableMapOf()

    // Must be resolved before updateState's reducer runs — it isn't suspend and can re-run on a CAS retry.
    private var cachedLastUsedAccountId: AccountId? = null

    private var rawCombos: List<FrequentCombo> = emptyList()

    private data class PendingPreselect(val accountId: String?, val categoryId: String?, val type: TransactionType?)

    private var pendingPreselect: PendingPreselect? = null

    // nav3 disposes and recreates this screen's composition on events the ViewModel survives —
    // rotation, and popping back from CategoryRoute in particular — which re-sends the same
    // OnPreselectCombo. Only the first registration may act; every repeat is a no-op.
    private var preselectConsumed: Boolean = false

    // False until init's combine has emitted at least once: an empty accounts/categories lookup
    // before that point means "not loaded yet", not "the id is gone" — only a lookup made once this
    // is true is allowed to give up on pendingPreselect.
    private var dataLoaded: Boolean = false

    // A preselected cold start can call loadFrequent twice — once from registerPreselect's type
    // switch, once from init's own default load — with different types racing; cancelling the
    // previous call keeps only the most recently requested type's combos.
    private var loadFrequentJob: Job? = null

    init {
        viewModelScope.launch {
            cachedLastUsedAccountId = loadOrNull { transactionStatsRepository.lastUsedAccountId() }

            combine(
                flow = accountRepository.all(),
                flow2 = categoryRepository.all().map(::mapToUi),
            ) { accounts, categories ->
                allCategories.clear()
                val categoryMap = categories.groupBy(SelectableCategory::categoryType).toMutableMap()
                allCategories.putAll(categoryMap)
                dataLoaded = true
                updateState {
                    copy(
                        accounts = accounts,
                        accountSelected = accountSelected ?: findLastUsedAccount(accounts, cachedLastUsedAccountId),
                        categories = allCategories[transactionType.categoryType].orEmpty(),
                        categorySelected = categorySelected
                            ?: allCategories[transactionType.categoryType]?.firstOrNull(),
                        frequentCombos = buildComboUi(rawCombos, accounts, allCategories),
                    )
                }
                resolvePendingPreselect(accounts)
            }.launchIn(viewModelScope)

            loadFrequent(currentState.transactionType)
        }
    }

    override fun onIntent(intent: AddTransactionIntent) {
        // Every interaction re-reads the clock, so a screen left open overnight stops claiming that
        // yesterday is "Hoy". StateFlow drops the emission when the day has not changed, which is
        // every intent but the handful that cross midnight.
        updateState { copy(today = today()) }
        when (intent) {
            is AddTransactionIntent.OnAmountChange -> updateState { copy(amount = intent.value) }

            is AddTransactionIntent.OnDescriptionChange -> updateState { copy(description = intent.value) }

            is AddTransactionIntent.OnTransactionTypeChange -> changeTransactionType(intent.value)

            is AddTransactionIntent.OnDateSelected -> updateState { copy(date = intent.value) }

            AddTransactionIntent.OnSave -> addTransaction()

            is AddTransactionIntent.OnAccountSelected -> updateState { copy(accountSelected = intent.value) }

            is AddTransactionIntent.OnCategorySelected -> updateState { copy(categorySelected = intent.value) }

            is AddTransactionIntent.OnFrequentComboSelected -> selectFrequentCombo(intent.value)

            is AddTransactionIntent.OnPreselectCombo -> registerPreselect(
                PendingPreselect(intent.accountId, intent.categoryId, intent.type),
            )

            is AddTransactionIntent.OnNewValueFromOthers -> addCategoryFromOthers(intent.value)
        }
    }

    private fun changeTransactionType(type: TransactionType) {
        updateState {
            copy(
                transactionType = type,
                categories = allCategories[type.categoryType].orEmpty(),
                categorySelected = allCategories[type.categoryType]?.firstOrNull(),
            )
        }
        loadFrequent(type)
    }

    private fun selectFrequentCombo(combo: FrequentComboUi) {
        val account = findAccount(currentState.accounts, combo.accountId)
        val category = findCategory(allCategories, combo.type.categoryType, combo.categoryId)
        if (account == null || category == null) return
        updateState {
            copy(
                accountSelected = account,
                categorySelected = category,
            )
        }
    }

    private fun registerPreselect(request: PendingPreselect) {
        if (preselectConsumed) return
        preselectConsumed = true
        if (request.accountId == null && request.categoryId == null && request.type == null) return
        pendingPreselect = request
        request.type?.let(::changeTransactionType)
        resolvePendingPreselect(currentState.accounts)
    }

    private fun resolvePendingPreselect(accounts: List<Account>) {
        val pending = pendingPreselect ?: return
        val categoryType = currentState.transactionType.categoryType
        val account = findAccount(accounts, pending.accountId)
        val category = findCategory(allCategories, categoryType, pending.categoryId)
        val accountMissing = pending.accountId != null && account == null
        val categoryMissing = pending.categoryId != null && category == null

        if (accountMissing || categoryMissing) {
            if (dataLoaded) pendingPreselect = null
        } else {
            pendingPreselect = null
            updateState {
                copy(
                    accountSelected = account ?: accountSelected,
                    categorySelected = category ?: categorySelected,
                )
            }
        }
    }

    /**
     * The schema refuses a cross-type (categoryId, type) pair; the route always carries the
     * movement's type, so this guard should never fire.
     */
    private fun addCategoryFromOthers(category: SelectableCategory) {
        val categoryType = currentState.transactionType.categoryType
        if (category.categoryType != categoryType) return
        val updatedCategories = allCategories[categoryType].orEmpty()
            .filterNot { it.categoryId == category.categoryId }
            .toMutableList()
            .apply { add(0, category) }
        updateState {
            copy(
                categories = updatedCategories,
                categorySelected = category,
            )
        }
    }

    private fun loadFrequent(type: TransactionType) {
        loadFrequentJob?.cancel()
        loadFrequentJob = viewModelScope.launch {
            val ids = loadOrNull { getTopUsedCategoryIds(type) }.orEmpty()
            updateState { copy(frequentCategoryIds = ids.map { it.value }) }

            rawCombos = loadOrNull { getFrequentCombos(type) }.orEmpty()
            val comboUiList = buildComboUi(rawCombos, currentState.accounts, allCategories)
            updateState { copy(frequentCombos = comboUiList) }
        }
    }

    private fun buildComboUi(
        combos: List<FrequentCombo>,
        accounts: List<Account>,
        categories: Map<CategoryType, List<SelectableCategory>>,
    ): List<FrequentComboUi> = combos.mapNotNull { combo ->
        val account = accounts.find { it.accountId == combo.accountId }
            ?: return@mapNotNull null
        val category = categories[combo.type.categoryType].orEmpty()
            .find { it.categoryId == combo.categoryId }
            ?: return@mapNotNull null
        FrequentComboUi(
            accountId = combo.accountId.value,
            categoryId = combo.categoryId.value,
            type = combo.type,
            label = comboLabel(account.name, category.name),
            colorId = category.colorId,
        )
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
            cachedLastUsedAccountId = currentState.accountSelected?.accountId
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

private fun findAccount(accounts: List<Account>, accountId: String?): Account? =
    accountId?.let { id -> accounts.find { it.accountId.value == id } }

private fun findCategory(
    categories: Map<CategoryType, List<SelectableCategory>>,
    categoryType: CategoryType,
    categoryId: String?,
): SelectableCategory? = categoryId?.let { id -> categories[categoryType].orEmpty().find { it.categoryId.value == id } }

private fun findLastUsedAccount(accounts: List<Account>, lastUsedAccountId: AccountId?): Account? =
    if (lastUsedAccountId != null) {
        accounts.find { it.accountId == lastUsedAccountId } ?: accounts.firstOrNull()
    } else {
        accounts.firstOrNull()
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
