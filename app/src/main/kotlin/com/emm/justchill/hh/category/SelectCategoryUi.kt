package com.emm.justchill.hh.category

import com.emm.justchill.hh.shared.Empty
import com.emm.justchill.hh.transaction.SelectableCategory

data class SelectCategoryUi(
    val allIncomes: List<SelectableCategory> = emptyList(),
    val allExpenses: List<SelectableCategory> = emptyList(),
    val filteredIncomes: List<SelectableCategory> = emptyList(),
    val filteredExpenses: List<SelectableCategory> = emptyList(),
    val query: String = String.Empty,
)