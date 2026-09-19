package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.domain.account.AccountRepository
import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.category.CategoryRepository
import com.emm.justchill.core.domain.shared.TransactionId
import com.emm.justchill.core.domain.time.TodayFlow
import com.emm.justchill.core.domain.transaction.DeleteTransactionUseCase
import com.emm.justchill.core.domain.transaction.GetTopUsedCategoryIdsUseCase
import com.emm.justchill.core.domain.transaction.Transaction
import com.emm.justchill.core.domain.transaction.TransactionRepository
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.domain.transaction.TransactionUpdate
import com.emm.justchill.core.domain.transaction.UpdateTransactionUseCase
import com.emm.justchill.core.ui.category.SelectableCategory
import com.emm.justchill.core.ui.category.toSelectable
import com.emm.justchill.core.ui.error.toUserMessage
import com.emm.justchill.core.ui.format.centsToMoney
import com.emm.justchill.core.ui.format.moneyCentsString
import com.emm.justchill.core.ui.mvi.MviViewModel
import com.emm.justchill.core.ui.transaction.Catalog
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDateTime

@Suppress("LongParameterList")
class EditTransactionViewModel(
    private val transactionId: String,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val updateTransaction: UpdateTransactionUseCase,
    private val transactionRepository: TransactionRepository,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val getTopUsedCategoryIds: GetTopUsedCategoryIdsUseCase,
    private val todayFlow: TodayFlow,
) : MviViewModel<EditTransactionUiState, EditTransactionIntent, EditTransactionEffect>(
    EditTransactionUiState(date = todayFlow.today(), today = todayFlow.today()),
) {

    init {
        combine(
            flow = accountRepository.all(),
            flow2 = categoryRepository.all().map { categories -> categories.map(Category::toSelectable) },
        ) { accounts, categories ->
            Catalog.Loaded(accounts, categories.groupBy(SelectableCategory::categoryType))
        }
            .onEach { loaded -> updateState { copy(catalog = loaded) } }
            .launchSafeIn(onError = { EditTransactionEffect.ShowError(it.toUserMessage()) })

        loadCurrentTransaction()
    }

    override fun onIntent(intent: EditTransactionIntent) {
        // Every interaction re-reads the date, so a screen left open overnight stops labelling
        // yesterday's transaction "Hoy". StateFlow drops the emission when the day has not changed.
        // Only the label moves: `date` is the transaction's own day and is never re-resolved.
        updateState { copy(today = todayFlow.today()) }
        when (intent) {
            is EditTransactionIntent.OnAmountChange -> updateState { copy(amount = intent.value) }
            is EditTransactionIntent.OnDescriptionChange -> updateState { copy(description = intent.value) }
            is EditTransactionIntent.OnTransactionTypeChange -> changeTransactionType(intent.value)
            is EditTransactionIntent.OnDateSelected -> updateState { copy(date = intent.value) }
            is EditTransactionIntent.OnAccountSelected -> updateState { copy(accountId = intent.value.accountId) }
            is EditTransactionIntent.OnCategorySelected -> updateState { copy(categoryId = intent.value.categoryId) }
            is EditTransactionIntent.OnNewValueFromOthers -> addCategoryFromOthers(intent.value)
            EditTransactionIntent.OnSave -> saveChanges()
            EditTransactionIntent.OnDeleteClick -> updateState { copy(showDeleteDialog = true) }
            EditTransactionIntent.OnDeleteDismiss -> updateState { copy(showDeleteDialog = false) }
            EditTransactionIntent.OnDeleteConfirm -> performDelete()
            is EditTransactionIntent.OnSheetRequested -> updateState { copy(openSheet = intent.sheet) }
            EditTransactionIntent.OnSheetDismissed -> updateState { copy(openSheet = null) }
        }
    }

    // The schema refuses a cross-type (categoryId, type) pair; the screen always asks for the
    // movement's own type, so this guard should never fire.
    private fun addCategoryFromOthers(category: SelectableCategory) {
        if (category.categoryType != currentState.transactionType.categoryType) return
        updateState {
            val others: List<SelectableCategory> = extraCategories.filterNot { it.categoryId == category.categoryId }
            copy(extraCategories = listOf(category) + others, categoryId = category.categoryId)
        }
    }

    // The category list is cut from the catalog per type at read time, so nothing here has to
    // clear a selection the new type cannot offer.
    private fun changeTransactionType(type: TransactionType) {
        updateState { copy(transactionType = type) }
        launchSafe(onError = { EditTransactionEffect.ShowError(it.toUserMessage()) }) { loadFrequent(type) }
    }

    private suspend fun loadFrequent(type: TransactionType) {
        val ids = loadOrNull { getTopUsedCategoryIds(type) }.orEmpty()
        updateState { copy(frequentCategoryIds = ids.map { it.value }) }
    }

    private fun loadCurrentTransaction() = launchSafe(
        onError = { EditTransactionEffect.ShowError(it.toUserMessage()) },
    ) {
        val stored: Transaction = transactionRepository.find(TransactionId(transactionId)) ?: return@launchSafe
        updateState {
            copy(
                original = stored,
                amount = moneyCentsString(stored.amount),
                description = stored.description,
                date = stored.occurredAt.date,
                transactionType = stored.type,
                accountId = stored.accountId,
                categoryId = stored.categoryId,
            )
        }
        loadFrequent(stored.type)
    }

    private fun saveChanges() = launchSafe(
        onError = { EditTransactionEffect.ShowError(it.toUserMessage()) },
    ) {
        val stored: Transaction = currentState.original ?: return@launchSafe
        updateTransaction(stored, currentState.toUpdate(stored))
        sendEffect(EditTransactionEffect.TransactionUpdated)
    }

    private fun performDelete() = launchSafe(
        onError = { EditTransactionEffect.ShowError(it.toUserMessage()) },
    ) {
        updateState { copy(showDeleteDialog = false) }
        deleteTransaction(TransactionId(transactionId))
        sendEffect(EditTransactionEffect.TransactionDeleted)
    }
}

// The hour comes from the row, never from the clock; and when the date is untouched, `date` still
// IS `stored.occurredAt.date`, so an amount-only edit rebuilds identical bytes by construction
// rather than through a special-cased branch.
private fun EditTransactionUiState.toUpdate(stored: Transaction): TransactionUpdate = TransactionUpdate(
    type = transactionType,
    description = description,
    occurredAt = LocalDateTime(date, stored.occurredAt.time),
    amount = centsToMoney(amount),
    accountId = accountSelected?.accountId
        ?: error("accountSelected required to build TransactionUpdate — UI should have disabled save"),
    categoryId = categorySelected?.categoryId,
)
