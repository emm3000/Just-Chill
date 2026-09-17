package com.emm.justchill.core.domain.transaction

import com.emm.justchill.core.domain.category.CategoryType

enum class TransactionType(val label: String, val categoryType: CategoryType) {

    Income(label = "Ingreso", categoryType = CategoryType.Income),
    Spend(label = "Gasto", categoryType = CategoryType.Spend),
}
