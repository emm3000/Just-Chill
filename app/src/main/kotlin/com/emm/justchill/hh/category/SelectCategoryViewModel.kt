package com.emm.justchill.hh.category

import androidx.lifecycle.viewModelScope
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.hh.transaction.SelectableCategory
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SelectCategoryViewModel(
    categoryRepository: CategoryRepository,
) : MviViewModel<SelectCategoryUi, SelectCategoryIntent, SelectCategoryEffect>() {

    override val initialState = SelectCategoryUi()

    init {
        categoryRepository.all()
            .onEach { categories ->
                val grouped = categories.groupBy { it.categoryType }
                updateState {
                    copy(
                        allIncomes = grouped[CategoryType.Income].orEmpty().map(::toSelectable),
                        allExpenses = grouped[CategoryType.Spend].orEmpty().map(::toSelectable),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: SelectCategoryIntent) {
        when (intent) {
            is SelectCategoryIntent.UpdateQuery -> updateState { copy(query = intent.value) }
            is SelectCategoryIntent.SelectType -> updateState { copy(selectedType = intent.value) }
        }
    }
}

private fun toSelectable(category: Category): SelectableCategory = SelectableCategory(
    categoryId = category.categoryId,
    name = category.name,
    icon = AppIconCatalog.findById(category.icon),
    color = findById(category.color),
    categoryType = category.categoryType,
)
