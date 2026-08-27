package com.emm.justchill.hh.category

import androidx.lifecycle.viewModelScope
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryUpsert
import com.emm.domain.category.DeleteCategoryUseCase
import com.emm.domain.category.UpdateCategoryUseCase
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class CategoriesViewModel(
    categoryRepository: CategoryRepository,
    transactionRepository: TransactionRepository,
    private val updateCategory: UpdateCategoryUseCase,
    private val deleteCategory: DeleteCategoryUseCase,
) : MviViewModel<CategoriesUiState, CategoriesIntent, CategoriesEffect>(CategoriesUiState()) {

    init {
        combine(
            categoryRepository.all(),
            transactionRepository.fetchAllWithCategory(),
        ) { categories, transactions ->
            val countByCategory = transactions
                .mapNotNull { it.category?.categoryId }
                .groupingBy { it }
                .eachCount()
            val uncategorizedSpend = transactions.count {
                it.category == null && it.type == TransactionType.Spend
            }
            Triple(categories, countByCategory, uncategorizedSpend)
        }
            .onEach { (categories, countByCategory, uncategorizedSpend) ->
                updateState {
                    copy(
                        categories = categories,
                        txCountByCategory = countByCategory,
                        uncategorizedSpendCount = uncategorizedSpend,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: CategoriesIntent) {
        when (intent) {
            is CategoriesIntent.OnEditClick -> updateState {
                copy(pendingEdit = intent.category, editName = intent.category.name)
            }

            is CategoriesIntent.OnEditNameChange -> updateState { copy(editName = intent.value) }

            CategoriesIntent.OnEditDismiss -> updateState { copy(pendingEdit = null, editName = "") }

            CategoriesIntent.OnEditConfirm -> confirmEdit()

            is CategoriesIntent.OnDeleteClick -> updateState { copy(pendingDelete = intent.category) }

            CategoriesIntent.OnDeleteDismiss -> updateState { copy(pendingDelete = null) }

            CategoriesIntent.OnDeleteConfirm -> confirmDelete()
        }
    }

    private fun confirmEdit() = launchSafe(
        onError = { e -> CategoriesEffect.ShowMessage(e.toUserMessage()) },
    ) {
        val target = currentState.pendingEdit ?: return@launchSafe
        val newName = currentState.editName
        updateCategory(
            categoryId = target.categoryId,
            categoryUpsert = CategoryUpsert(
                categoryId = target.categoryId,
                name = newName,
                icon = target.icon,
                color = target.color,
                categoryType = target.categoryType,
            ),
        )
        updateState { copy(pendingEdit = null, editName = "") }
    }

    private fun confirmDelete() = launchSafe(
        onError = { e ->
            updateState { copy(pendingDelete = null) }
            CategoriesEffect.ShowMessage(e.toUserMessage())
        },
    ) {
        val target = currentState.pendingDelete ?: return@launchSafe
        deleteCategory(target.categoryId)
        updateState { copy(pendingDelete = null) }
    }
}
