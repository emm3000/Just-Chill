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
import com.emm.justchill.hh.shared.Empty
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

    // Cached last-used account id — resolved before the combine flow fires.
    private var cachedLastUsedAccountId: AccountId? = null

    // Raw domain combos for the current type — resolved by loadFrequent, re-mapped when accounts/categories update.
    private var rawCombos: List<FrequentCombo> = emptyList()

    init {
        viewModelScope.launch {
            cachedLastUsedAccountId = runCatching { transactionStatsRepository.lastUsedAccountId() }.getOrNull()

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
        // Every interaction re-reads the clock, so a screen left open overnight stops claiming that
        // yesterday is "Hoy". StateFlow drops the emission when the day has not changed, which is
        // every intent but the handful that cross midnight.
        updateState { copy(today = today()) }
        when (intent) {
            is AddTransactionIntent.OnAmountChange -> updateState { copy(amount = intent.value).touched() }

            is AddTransactionIntent.OnDescriptionChange -> updateState { copy(description = intent.value).touched() }

            is AddTransactionIntent.OnTransactionTypeChange -> changeTransactionType(intent.value)

            is AddTransactionIntent.OnDateSelected -> updateState { copy(date = intent.value).touched() }

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
        // Scoped to the combo's own type, not to every category the app has: a combo carries the
        // type it was recorded under, and the pair it restores has to be one the schema accepts.
        val category = allCategories[combo.type.categoryType].orEmpty()
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
        updateState {
            val defaultType = TransactionType.Income
            copy(
                amount = "",
                description = String.Empty,
                date = null,
                today = today(),
                transactionType = defaultType,
                categories = allCategories[defaultType.categoryType].orEmpty(),
                categorySelected = allCategories[defaultType.categoryType]?.firstOrNull(),
                accountSelected = resolveLastUsedAccount(accounts),
                isEnabled = false,
                hasChanges = false,
            )
        }
    }

    /**
     * Takes the category the new-category screen just created and selects it here.
     *
     * Scoped to the movement's own type, twice over. The list it rebuilds used to be every category
     * in the app flattened, so creating one from an Income movement replaced the Income picker with
     * Income AND Spend entries; and a category of the other type is now a pair the schema refuses
     * outright, so selecting it would only produce a failed save. The route carries the movement's
     * type, so the guard should never fire — it is what keeps this correct if a caller forgets to.
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
        // Same scoping as selectFrequentCombo: a chip that renders must be a chip that can be
        // applied, and the pair behind it has to be one the schema accepts.
        val category = categories[combo.type.categoryType].orEmpty()
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

    // The day is the user's, the hour is the moment of the save — and both are decided HERE,
    // where the save happens, not against the state's `today`. Resolving them earlier is what
    // booked a screen opened at 23:59 and saved at 00:01 on the previous day.
    //
    // This is one of the two legitimate reads of a timezone left in the app: "what is the local
    // date and time for this user, right now". Everything downstream carries the answer, not the
    // question — the use case validates it and the column stores it, neither converts it.
    private fun createTransactionInsert(): TransactionInsert {
        val now: LocalDateTime = clock.now().toLocalDateTime(zone)
        return TransactionInsert(
            type = currentState.transactionType,
            description = currentState.description,
            occurredAt = LocalDateTime(currentState.date ?: now.date, now.time),
            amount = centsToMoney(currentState.amount),
            categoryId = currentState.categorySelected?.categoryId,
            accountId = currentState.accountSelected?.accountId
                ?: error("accountSelected required to build TransactionInsert — UI should have disabled save"),
        )
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

private fun AddTransactionUiState.validate(): AddTransactionUiState = copy(isEnabled = missingField == null)

private fun AddTransactionUiState.touched(): AddTransactionUiState = validate().copy(hasChanges = true)
