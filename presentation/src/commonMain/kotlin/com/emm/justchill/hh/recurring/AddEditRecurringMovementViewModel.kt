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
import com.emm.justchill.hh.transaction.isSavableAmount
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
                    val resolved = when {
                        pendingAccountId != null ->
                            accounts.find { it.accountId.value == pendingAccountId }
                                ?: selectedAccount

                        selectedAccount != null ->
                            accounts.find { it.accountId.value == selectedAccount.accountId.value }
                                ?: selectedAccount

                        else -> accounts.firstOrNull()
                    }
                    // Scoped to the type's own categories: the other type's pending id is a pair the schema refuses.
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
            accountId = AccountId(s.selectedAccount?.accountId?.value.orEmpty()),
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
    val amountValid = isVariableAmount || amountDigits.isSavableAmount()
    return copy(isSaveEnabled = name.isNotBlank() && selectedAccount != null && amountValid)
}

private fun mapCategory(c: Category): SelectableCategory = SelectableCategory(
    categoryId = c.categoryId,
    name = c.name,
    iconId = c.icon,
    categoryType = c.categoryType,
    colorId = c.color,
)
