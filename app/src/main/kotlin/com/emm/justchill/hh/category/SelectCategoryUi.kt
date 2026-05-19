package com.emm.justchill.hh.category

import com.emm.domain.category.CategoryType
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.shared.Empty
import com.emm.justchill.hh.transaction.SelectableCategory

data class SelectCategoryUi(
    val allIncomes: List<SelectableCategory> = emptyList(),
    val allExpenses: List<SelectableCategory> = emptyList(),
    val query: String = String.Empty,
    val selectedType: CategoryType = CategoryType.Spend,
) : UiState {
    val activeList: List<SelectableCategory>
        get() = when (selectedType) {
            CategoryType.Income -> allIncomes
            CategoryType.Spend -> allExpenses
        }

    val filteredActive: List<SelectableCategory>
        get() = if (query.isBlank()) activeList
        else activeList.filter { it.name.contains(query.trim(), ignoreCase = true) }
}