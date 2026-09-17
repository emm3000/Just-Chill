package com.emm.justchill.hh.category

import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.mvi.UiState

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val txCountByCategory: Map<CategoryId, Int> = emptyMap(),
    val uncategorizedSpendCount: Int = 0,
    val pendingEdit: Category? = null,
    val editName: String = "",
    val pendingDelete: Category? = null,
) : UiState
