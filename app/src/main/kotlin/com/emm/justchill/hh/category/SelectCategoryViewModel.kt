package com.emm.justchill.hh.category

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.justchill.hh.transaction.SelectableCategory
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class SelectCategoryViewModel(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    var state by mutableStateOf(SelectCategoryUi())
        private set

    init {
        fetchAll()
    }

    fun fetchAll() = categoryRepository.all()
        .map(::mapToUiAndPartitionByType)
        .onEach {
            state = state.copy(
                allIncomes = it.first,
                allExpenses = it.second,
                filteredIncomes = it.first,
                filteredExpenses = it.second,
            )
        }
        .launchIn(viewModelScope)
}

private fun mapToUiAndPartitionByType(
    categories: List<Category>,
): Pair<List<SelectableCategory>, List<SelectableCategory>> = categories.map { category ->
    SelectableCategory(
        categoryId = category.categoryId,
        name = category.name,
        icon = AppIconCatalog.findById(category.icon),
        color = findById(category.color),
        categoryType = category.categoryType,
    )
}.filter { selectableCategory ->
    selectableCategory.categoryType != CategoryType.Both
}.partition { selectableCategory ->
    selectableCategory.categoryType == CategoryType.Income
}