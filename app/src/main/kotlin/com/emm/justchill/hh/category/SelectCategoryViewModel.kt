package com.emm.justchill.hh.category

import androidx.lifecycle.viewModelScope
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.hh.transaction.SelectableCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SelectCategoryViewModel(
    private val categoryRepository: CategoryRepository,
) : MviViewModel<SelectCategoryUi, SelectCategoryIntent, SelectCategoryEffect>() {

    override val initialState = SelectCategoryUi()

    private val queryFlow = MutableStateFlow("")

    init {
        queryFlow
            .debounce(220L)
            .distinctUntilChanged()
            .flatMapLatest { searchQuery ->
                flow {
                    if (searchQuery.isBlank()) {
                        emit(Pair(currentState.allIncomes, currentState.allExpenses))
                        return@flow
                    }
                    val filteredIncomes = currentState.allIncomes.filter { it.name.contains(searchQuery, ignoreCase = true) }
                    val filteredExpenses = currentState.allExpenses.filter { it.name.contains(searchQuery, ignoreCase = true) }
                    emit(Pair(filteredIncomes, filteredExpenses))
                }.flowOn(Dispatchers.Default)
            }
            .onEach { (incomes, expenses) ->
                updateState { copy(filteredIncomes = incomes, filteredExpenses = expenses) }
            }
            .launchIn(viewModelScope)

        categoryRepository.all()
            .map(::mapToUiAndPartitionByType)
            .onEach { map ->
                updateState {
                    copy(
                        allIncomes = map[CategoryType.Income].orEmpty(),
                        allExpenses = map[CategoryType.Spend].orEmpty(),
                        filteredIncomes = map[CategoryType.Income].orEmpty(),
                        filteredExpenses = map[CategoryType.Spend].orEmpty(),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: SelectCategoryIntent) {
        when (intent) {
            is SelectCategoryIntent.UpdateQuery -> {
                updateState { copy(query = intent.value) }
                queryFlow.value = intent.value
            }
        }
    }
}

private fun mapToUiAndPartitionByType(
    categories: List<Category>,
): Map<CategoryType, List<SelectableCategory>> {
    val result = mutableMapOf<CategoryType, MutableList<SelectableCategory>>()
    categories.forEach { category ->
        val ui = SelectableCategory(
            categoryId = category.categoryId,
            name = category.name,
            icon = AppIconCatalog.findById(category.icon),
            color = findById(category.color),
            categoryType = category.categoryType,
        )
        result.getOrPut(ui.categoryType) { mutableListOf() }.add(ui)
    }
    return result
}
