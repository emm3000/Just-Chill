package com.emm.justchill.hh.category

import androidx.compose.runtime.Stable
import com.emm.domain.category.Category
import com.emm.domain.shared.CategoryId
import com.emm.justchill.core.mvi.UiState

@Stable
data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val txCountByCategory: Map<CategoryId, Int> = emptyMap(),
    val uncategorizedSpendCount: Int = 0,
    val pendingEdit: Category? = null,
    val editName: String = "",
    val pendingDelete: Category? = null,
) : UiState
