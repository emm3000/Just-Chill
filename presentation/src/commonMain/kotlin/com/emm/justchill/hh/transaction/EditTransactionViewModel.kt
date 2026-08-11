package com.emm.justchill.hh.transaction

import androidx.lifecycle.viewModelScope
import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.FindAccountUseCase
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.TransactionId
import com.emm.domain.transaction.DeleteTransactionUseCase
import com.emm.domain.transaction.FindTransactionUseCase
import com.emm.domain.transaction.GetTopUsedCategoryIdsUseCase
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionUpdate
import com.emm.domain.transaction.UpdateTransactionUseCase
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Suppress("LongParameterList")
class EditTransactionViewModel(
    private val transactionId: String,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val updateTransaction: UpdateTransactionUseCase,
    private val findTransaction: FindTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val findAccount: FindAccountUseCase,
    private val getTopUsedCategoryIds: GetTopUsedCategoryIdsUseCase,
    private val clock: Clock = Clock.System,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : MviViewModel<EditTransactionUiState, EditTransactionIntent, EditTransactionEffect>() {

    override val initialState = EditTransactionUiState(date = today(), today = today())

    private var oldTransaction: Transaction = Transaction.Empty

    private var snapshot: Snapshot? = null
    private val allCategories: MutableMap<CategoryType, List<SelectableCategory>> = mutableMapOf()

    init {
        loadCurrentTransaction()
    }

    override fun onIntent(intent: EditTransactionIntent) {
        // Every interaction re-reads the clock, so a screen left open overnight stops labelling
        // yesterday's transaction "Hoy". StateFlow drops the emission when the day has not changed.
        // Only the label moves: `date` is the transaction's own day and is never re-resolved.
        updateState { copy(today = today()) }
        when (intent) {
            is EditTransactionIntent.OnAmountChange -> updateState { copy(amount = intent.value).recompute() }

            is EditTransactionIntent.OnDescriptionChange -> updateState { copy(description = intent.value).recompute() }

            is EditTransactionIntent.OnTransactionTypeChange -> changeTransactionType(intent.value)

            is EditTransactionIntent.OnDateSelected -> updateState { copy(date = intent.value).recompute() }

            is EditTransactionIntent.OnAccountSelected -> updateState {
                copy(
                    accountSelected = intent.value,
                ).recompute()
            }

            is EditTransactionIntent.OnCategorySelected -> updateState {
                copy(
                    categorySelected = intent.value,
                ).recompute()
            }

            EditTransactionIntent.OnSave -> saveChanges()

            EditTransactionIntent.OnDelete -> performDelete()
        }
    }

    private fun changeTransactionType(type: TransactionType) {
        updateState {
            val list = allCategories[type.categoryType].orEmpty()
            copy(
                transactionType = type,
                categories = list,
                categorySelected = list.firstOrNull { it.categoryId == snapshot?.categoryId }
                    ?: list.firstOrNull(),
            ).recompute()
        }
        loadFrequent(type)
    }

    private fun loadFrequent(type: TransactionType) = viewModelScope.launch {
        val ids = runCatching { getTopUsedCategoryIds(type) }.getOrDefault(emptyList())
        updateState { copy(frequentCategoryIds = ids.map { it.value }) }
    }

    private fun EditTransactionUiState.recompute(): EditTransactionUiState {
        val snap = snapshot ?: return copy(isEnabled = false, hasChanges = false)
        val changed = amount != snap.amount ||
            description != snap.description ||
            date != snap.date ||
            transactionType != snap.type ||
            accountSelected?.accountId != snap.accountId ||
            categorySelected?.categoryId != snap.categoryId
        return copy(
            hasChanges = changed,
            isEnabled = changed && missingField == null,
        )
    }

    private fun loadCurrentTransaction() = viewModelScope.launch {
        val accounts: List<Account> = accountRepository.all().firstOrNull() ?: emptyList()
        val categoriesList = categoryRepository.all().firstOrNull().orEmpty().map(::toSelectable)
        allCategories.clear()
        allCategories.putAll(categoriesList.groupBy(SelectableCategory::categoryType))

        oldTransaction = findTransaction(TransactionId(transactionId)) ?: return@launch
        val account = findAccount(oldTransaction.accountId) ?: return@launch
        val storedDay: LocalDate = oldTransaction.occurredAt.date

        val selectedCategory: SelectableCategory? = oldTransaction.categoryId?.let { id ->
            categoriesList.firstOrNull { it.categoryId == id }
        }
        val categoriesForType = allCategories[oldTransaction.type.categoryType].orEmpty()

        snapshot = Snapshot(
            amount = moneyCentsString(oldTransaction.amount),
            description = oldTransaction.description,
            date = storedDay,
            type = oldTransaction.type,
            accountId = account.accountId,
            categoryId = oldTransaction.categoryId,
        )

        updateState {
            copy(
                amount = moneyCentsString(oldTransaction.amount),
                description = oldTransaction.description,
                date = storedDay,
                transactionType = oldTransaction.type,
                accounts = accounts,
                accountSelected = account,
                categories = categoriesForType,
                categorySelected = selectedCategory ?: categoriesForType.firstOrNull(),
                isEnabled = false,
                hasChanges = false,
            )
        }
        loadFrequent(oldTransaction.type)
    }

    private fun saveChanges() = launchSafe(
        onError = { EditTransactionEffect.ShowError(it.toUserMessage()) },
    ) {
        updateTransaction(oldTransaction, createTransactionUpdate())
        sendEffect(EditTransactionEffect.TransactionUpdated)
    }

    // The day the user is looking at, carrying the hour the transaction was already recorded at:
    // moving a movement to another day must not restamp when it happened.
    //
    // No timezone and no instant, which is the whole point. If the user did not touch the date,
    // `currentState.date` still IS `oldTransaction.occurredAt.date`, so this rebuilds the stored
    // value exactly — an amount-only edit writes back identical bytes by construction, not because
    // some branch remembered to leave the date alone. That branch is what corrupted the date twice.
    private fun createTransactionUpdate(): TransactionUpdate = TransactionUpdate(
        type = currentState.transactionType,
        description = currentState.description,
        occurredAt = LocalDateTime(currentState.date, oldTransaction.occurredAt.time),
        amount = centsToMoney(currentState.amount),
        accountId = currentState.accountSelected?.accountId
            ?: error("accountSelected required to build TransactionUpdate — UI should have disabled save"),
        categoryId = currentState.categorySelected?.categoryId,
    )

    private fun performDelete() = launchSafe(
        onError = { EditTransactionEffect.ShowError(it.toUserMessage()) },
    ) {
        deleteTransaction(oldTransaction.transactionId)
        sendEffect(EditTransactionEffect.TransactionDeleted)
    }

    private fun today(): LocalDate = clock.now().toLocalDateTime(zone).date

    private data class Snapshot(
        val amount: String,
        val description: String,
        val date: LocalDate,
        val type: TransactionType,
        val accountId: AccountId,
        val categoryId: CategoryId?,
    )
}

private fun toSelectable(c: Category): SelectableCategory = SelectableCategory(
    categoryId = c.categoryId,
    name = c.name,
    iconId = c.icon,
    categoryType = c.categoryType,
    colorId = c.color,
)
