package com.emm.justchill.hh.transaction

import androidx.lifecycle.viewModelScope
import com.emm.domain.account.AccountRepository
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.shared.TransactionId
import com.emm.domain.transaction.DeleteTransactionUseCase
import com.emm.domain.transaction.GetTopUsedCategoryIdsUseCase
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionUpdate
import com.emm.domain.transaction.UpdateTransactionUseCase
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import kotlinx.coroutines.flow.combine
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
class EditTransactionViewModel(
    private val transactionId: String,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val updateTransaction: UpdateTransactionUseCase,
    private val transactionRepository: TransactionRepository,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val getTopUsedCategoryIds: GetTopUsedCategoryIdsUseCase,
    private val clock: Clock,
    private val zone: TimeZone,
) : MviViewModel<EditTransactionUiState, EditTransactionIntent, EditTransactionEffect>() {

    override val initialState = EditTransactionUiState(date = today(), today = today())

    init {
        combine(
            flow = accountRepository.all(),
            flow2 = categoryRepository.all().map { categories -> categories.map(Category::toSelectable) },
        ) { accounts, categories ->
            Catalog.Loaded(accounts, categories.groupBy(SelectableCategory::categoryType))
        }
            .onEach { loaded -> updateState { copy(catalog = loaded) } }
            .launchIn(viewModelScope)

        loadCurrentTransaction()
    }

    override fun onIntent(intent: EditTransactionIntent) {
        // Every interaction re-reads the clock, so a screen left open overnight stops labelling
        // yesterday's transaction "Hoy". StateFlow drops the emission when the day has not changed.
        // Only the label moves: `date` is the transaction's own day and is never re-resolved.
        updateState { copy(today = today()) }
        when (intent) {
            is EditTransactionIntent.OnAmountChange -> updateState { copy(amount = intent.value) }
            is EditTransactionIntent.OnDescriptionChange -> updateState { copy(description = intent.value) }
            is EditTransactionIntent.OnTransactionTypeChange -> changeTransactionType(intent.value)
            is EditTransactionIntent.OnDateSelected -> updateState { copy(date = intent.value) }
            is EditTransactionIntent.OnAccountSelected -> updateState { copy(accountId = intent.value.accountId) }
            is EditTransactionIntent.OnCategorySelected -> updateState { copy(categoryId = intent.value.categoryId) }
            EditTransactionIntent.OnSave -> saveChanges()
            EditTransactionIntent.OnDelete -> performDelete()
        }
    }

    // The category list is cut from the catalog per type at read time, so nothing here has to
    // clear a selection the new type cannot offer.
    private fun changeTransactionType(type: TransactionType) {
        updateState { copy(transactionType = type) }
        loadFrequent(type)
    }

    private fun loadFrequent(type: TransactionType) = viewModelScope.launch {
        val ids = loadOrNull { getTopUsedCategoryIds(type) }.orEmpty()
        updateState { copy(frequentCategoryIds = ids.map { it.value }) }
    }

    private fun loadCurrentTransaction() = viewModelScope.launch {
        val stored: Transaction = transactionRepository.find(TransactionId(transactionId)) ?: return@launch
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
        deleteTransaction(TransactionId(transactionId))
        sendEffect(EditTransactionEffect.TransactionDeleted)
    }

    private fun today(): LocalDate = clock.now().toLocalDateTime(zone).date
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
