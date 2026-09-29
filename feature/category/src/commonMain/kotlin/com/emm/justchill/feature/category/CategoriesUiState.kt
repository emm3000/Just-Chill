package com.emm.justchill.feature.category

import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.presentation.mvi.UiState

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val txCountByCategory: Map<CategoryId, Int> = emptyMap(),
    val uncategorizedSpendCount: Int = 0,
    val pendingEdit: Category? = null,
    val editName: String = "",
    val pendingDelete: Category? = null,
) : UiState {

    val incomeRows: List<CategoryRowUi> get() = rowsOf(CategoryType.Income)

    val spendRows: List<CategoryRowUi> get() = rowsOf(CategoryType.Spend)

    val spendSectionCount: Int get() = spendRows.size + 1

    val uncategorizedCountLabel: String get() = movementCountLabel(uncategorizedSpendCount)

    val pendingDeleteMessage: String?
        get() = pendingDelete?.let { category: Category -> deleteCategoryMessage(movementCountOf(category)) }

    private fun rowsOf(type: CategoryType): List<CategoryRowUi> = categories
        .filter { it.categoryType == type }
        .sortedBy { it.name.lowercase() }
        .map { category: Category -> CategoryRowUi(category, movementCountOf(category)) }

    private fun movementCountOf(category: Category): Int = txCountByCategory[category.categoryId] ?: 0
}

private fun deleteCategoryMessage(affectedCount: Int): String = when (affectedCount) {
    0 -> "Ningún movimiento la usa, así que no cambia nada de tu historial."
    1 -> "1 movimiento va a quedar sin categoría. No puedes deshacerlo desde la app."
    else -> "$affectedCount movimientos van a quedar sin categoría. No puedes deshacerlo desde la app."
}
