package com.emm.justchill.hh.recurring

import com.emm.domain.account.AccountRepository
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.recurring.CreateRecurringMovementUseCase
import com.emm.domain.recurring.RecurringMovement
import com.emm.domain.recurring.RecurringMovementInsert
import com.emm.domain.recurring.RecurringMovementRepository
import com.emm.domain.recurring.UpdateRecurringMovementUseCase
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.hh.transaction.Catalog
import com.emm.justchill.hh.transaction.SelectableCategory
import com.emm.justchill.hh.transaction.centsToMoney
import com.emm.justchill.hh.transaction.moneyCentsString
import com.emm.justchill.hh.transaction.toSelectable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class AddEditRecurringMovementViewModel(
    private val id: String?,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val recurringRepository: RecurringMovementRepository,
    private val createRecurring: CreateRecurringMovementUseCase,
    private val updateRecurring: UpdateRecurringMovementUseCase,
) : MviViewModel<AddEditRecurringMovementUiState, AddEditRecurringMovementIntent, AddEditRecurringMovementEffect>(
    AddEditRecurringMovementUiState(isEdit = id != null),
) {

    init {
        combine(
            flow = accountRepository.all(),
            flow2 = categoryRepository.all().map { categories -> categories.map(Category::toSelectable) },
        ) { accounts, categories ->
            Catalog.Loaded(accounts, categories.groupBy(SelectableCategory::categoryType))
        }
            .onEach { loaded -> updateState { copy(catalog = loaded) } }
            .launchSafeIn(onError = { e -> AddEditRecurringMovementEffect.ShowError(e.toUserMessage()) })

        if (id != null) {
            launchSafe(onError = { e -> AddEditRecurringMovementEffect.ShowError(e.toUserMessage()) }) {
                loadTemplate(id)
            }
        }
    }

    override fun onIntent(intent: AddEditRecurringMovementIntent) {
        when (intent) {
            is AddEditRecurringMovementIntent.OnNameChange -> updateState { copy(name = intent.value) }

            // The list is cut per type at read time, so a Spend category simply stops being
            // offered on an Income template — the pair the schema refuses cannot survive here.
            is AddEditRecurringMovementIntent.OnTypeChange -> updateState { copy(type = intent.value) }

            is AddEditRecurringMovementIntent.OnAmountChange -> updateState { copy(amountDigits = intent.digits) }

            is AddEditRecurringMovementIntent.OnVariableAmountToggle -> {
                updateState { copy(isVariableAmount = intent.isVariable) }
            }

            is AddEditRecurringMovementIntent.OnDayOfMonthChange -> updateState { copy(dayOfMonth = intent.day) }

            is AddEditRecurringMovementIntent.OnIsActiveChange -> updateState { copy(isActive = intent.isActive) }

            is AddEditRecurringMovementIntent.OnDescriptionChange -> updateState { copy(description = intent.value) }

            is AddEditRecurringMovementIntent.OnAccountSelected -> {
                updateState { copy(accountId = intent.account.accountId) }
            }

            is AddEditRecurringMovementIntent.OnCategorySelected -> {
                updateState { copy(categoryId = intent.category?.categoryId) }
            }

            AddEditRecurringMovementIntent.Save -> save()
        }
    }

    private suspend fun loadTemplate(templateId: String) {
        val template: RecurringMovement = recurringRepository.find(RecurringMovementId(templateId)) ?: return
        val fixedAmount: Money? = template.amount
        updateState {
            copy(
                name = template.name,
                type = template.type,
                amountDigits = if (fixedAmount != null) moneyCentsString(fixedAmount) else "",
                isVariableAmount = fixedAmount == null,
                dayOfMonth = template.dayOfMonth,
                isActive = template.isActive,
                description = template.description,
                accountId = template.accountId,
                categoryId = template.categoryId,
            )
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
            // The RESOLVED selection, so a category deleted while the form was open is written as
            // "sin categoría" instead of as an id whose row is gone.
            categoryId = s.selectedCategory?.categoryId,
            accountId = s.selectedAccount?.accountId
                ?: error("selectedAccount required to build the insert — UI should have disabled save"),
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
