package com.emm.justchill.hh.recurring

import androidx.lifecycle.viewModelScope
import com.emm.domain.account.AccountRepository
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.domain.recurring.CreateRecurringMovementUseCase
import com.emm.domain.recurring.RecurringMovement
import com.emm.domain.recurring.RecurringMovementInsert
import com.emm.domain.recurring.RecurringMovementRepository
import com.emm.domain.recurring.UpdateRecurringMovementUseCase
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.hh.transaction.SelectableCategory
import com.emm.justchill.hh.transaction.centsToMoney
import com.emm.justchill.hh.transaction.moneyCentsString
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AddEditRecurringMovementViewModel(
    private val id: String?,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val recurringRepository: RecurringMovementRepository,
    private val createRecurring: CreateRecurringMovementUseCase,
    private val updateRecurring: UpdateRecurringMovementUseCase,
) : MviViewModel<AddEditRecurringMovementUiState, AddEditRecurringMovementIntent, AddEditRecurringMovementEffect>() {

    override val initialState = AddEditRecurringMovementUiState(isEdit = id != null)

    /**
     * Every category the app has, grouped by type — the source the state's own list is cut from.
     *
     * A template mints a transaction of its own type every month, and `(categoryId, type)` is a
     * foreign key since schema v5, so a template holding a category of the other type is a
     * generator of movements the database refuses. The form therefore only ever offers the
     * categories of the type currently selected, and this map is what makes switching the type
     * able to re-cut that list without another read.
     */
    private val allCategories: MutableMap<CategoryType, List<SelectableCategory>> = mutableMapOf()

    init {
        combine(
            accountRepository.all(),
            categoryRepository.all(),
        ) { accounts, categories ->
            accounts to categories.map(::mapCategory)
        }
            .onEach { (accounts, categories) ->
                allCategories.clear()
                allCategories.putAll(categories.groupBy(SelectableCategory::categoryType))
                updateState {
                    // Resolve the selected account from the loaded list.
                    // - Create mode: default to first account.
                    // - Edit mode: either already resolved (selectedAccount != null and present
                    //   in the new list) OR pending resolution via pendingAccountId.
                    val resolved = when {
                        pendingAccountId != null ->
                            accounts.find { it.accountId.value == pendingAccountId }
                                ?: selectedAccount

                        selectedAccount != null ->
                            accounts.find { it.accountId.value == selectedAccount.accountId.value }
                                ?: selectedAccount

                        else -> accounts.firstOrNull()
                    }
                    // Resolve the selected category the same way the account is resolved, so an
                    // edit-mode load-ordering race (categories not yet emitted) does not drop it.
                    // Out of the type's own categories: a pending id of the other type is a pair
                    // the schema refuses, so resolving it would only restore an unsavable form.
                    val forType = categoriesFor(type)
                    val resolvedCategory = when {
                        pendingCategoryId != null ->
                            forType.find { it.categoryId.value == pendingCategoryId }
                                ?: selectedCategory

                        else -> selectedCategory
                    }
                    copy(
                        accounts = accounts,
                        categories = forType,
                        selectedAccount = resolved,
                        selectedCategory = resolvedCategory,
                        // Clear pending once resolved or if accounts loaded empty (will retry next emit).
                        pendingAccountId = if (resolved != null) null else pendingAccountId,
                        pendingCategoryId = if (resolvedCategory != null) null else pendingCategoryId,
                    ).recalcSaveEnabled()
                }
            }
            .launchIn(viewModelScope)

        if (id != null) {
            viewModelScope.launch { loadTemplate(id) }
        }
    }

    override fun onIntent(intent: AddEditRecurringMovementIntent) {
        when (intent) {
            is AddEditRecurringMovementIntent.OnNameChange -> {
                updateState { copy(name = intent.value).recalcSaveEnabled() }
            }

            is AddEditRecurringMovementIntent.OnTypeChange -> changeType(intent.value)

            is AddEditRecurringMovementIntent.OnAmountChange -> {
                updateState { copy(amountDigits = intent.digits).recalcSaveEnabled() }
            }

            is AddEditRecurringMovementIntent.OnVariableAmountToggle -> {
                updateState { copy(isVariableAmount = intent.isVariable).recalcSaveEnabled() }
            }

            is AddEditRecurringMovementIntent.OnDayOfMonthChange -> {
                updateState { copy(dayOfMonth = intent.day) }
            }

            is AddEditRecurringMovementIntent.OnIsActiveChange -> {
                updateState { copy(isActive = intent.isActive) }
            }

            is AddEditRecurringMovementIntent.OnDescriptionChange -> {
                updateState { copy(description = intent.value) }
            }

            is AddEditRecurringMovementIntent.OnAccountSelected -> {
                updateState { copy(selectedAccount = intent.account).recalcSaveEnabled() }
            }

            is AddEditRecurringMovementIntent.OnCategorySelected -> {
                updateState { copy(selectedCategory = intent.category) }
            }

            AddEditRecurringMovementIntent.Save -> save()
        }
    }

    /**
     * Switching the type re-cuts the category list and DROPS the selection.
     *
     * Keeping it is the exact bug this whole change exists to remove: a template switched to
     * Income while still holding a Spend category is a pair the schema refuses, and the failure
     * would surface at save time as a generic database error rather than here, where the user can
     * see what happened. The field is optional, so an empty selection is a state the form already
     * renders ("Sin categoría").
     */
    private fun changeType(type: TransactionType) = updateState {
        copy(
            type = type,
            categories = categoriesFor(type),
            selectedCategory = null,
            pendingCategoryId = null,
        ).recalcSaveEnabled()
    }

    private fun categoriesFor(type: TransactionType): List<SelectableCategory> =
        allCategories[type.categoryType].orEmpty()

    private suspend fun loadTemplate(templateId: String) {
        val template: RecurringMovement = recurringRepository.find(RecurringMovementId(templateId)) ?: return
        val fixedAmount: Money? = template.amount
        updateState {
            // Try to resolve the account from the already-loaded accounts list.
            // If accounts have not yet been emitted by the combine flow, store the id in
            // pendingAccountId; the combine collector will resolve it on next emission.
            val resolvedAccount = accounts.find { it.accountId.value == template.accountId.value }
            val forType = categoriesFor(template.type)
            val resolvedCategory = template.categoryId?.let { categoryId ->
                forType.find { it.categoryId.value == categoryId.value }
            }
            copy(
                name = template.name,
                type = template.type,
                categories = forType,
                amountDigits = if (fixedAmount != null) moneyCentsString(fixedAmount) else "",
                isVariableAmount = fixedAmount == null,
                dayOfMonth = template.dayOfMonth,
                isActive = template.isActive,
                description = template.description,
                selectedAccount = resolvedAccount,
                pendingAccountId = if (resolvedAccount == null) template.accountId.value else null,
                selectedCategory = resolvedCategory,
                // Defer resolution to the combine collector when categories have not loaded yet.
                pendingCategoryId = if (resolvedCategory == null) template.categoryId?.value else null,
            ).recalcSaveEnabled()
        }
    }

    private fun save() = launchSafe(
        onError = { e -> AddEditRecurringMovementEffect.ShowError(e.toUserMessage()) },
    ) {
        val s = currentState
        val insert = RecurringMovementInsert(
            name = s.name,
            type = s.type,
            amount = if (s.isVariableAmount) null else resolveAmount(s.amountDigits),
            description = s.description,
            categoryId = s.selectedCategory?.categoryId?.let { CategoryId(it.value) },
            accountId = AccountId(s.selectedAccount?.accountId?.value ?: ""),
            dayOfMonth = s.dayOfMonth,
            isActive = s.isActive,
        )
        val templateId = id
        if (templateId != null) {
            updateRecurring(RecurringMovementId(templateId), insert)
        } else {
            createRecurring(insert)
        }
        sendEffect(AddEditRecurringMovementEffect.NavigateBack)
    }

    private fun resolveAmount(digits: String): Money? = if (digits.isEmpty()) null else centsToMoney(digits)
}

private fun AddEditRecurringMovementUiState.recalcSaveEnabled(): AddEditRecurringMovementUiState {
    val amountValid = isVariableAmount || (amountDigits.isNotEmpty() && amountDigits.toLongOrNull() != 0L)
    return copy(isSaveEnabled = name.isNotBlank() && selectedAccount != null && amountValid)
}

private fun mapCategory(c: Category): SelectableCategory = SelectableCategory(
    categoryId = c.categoryId,
    name = c.name,
    iconId = c.icon,
    categoryType = c.categoryType,
    colorId = c.color,
)
