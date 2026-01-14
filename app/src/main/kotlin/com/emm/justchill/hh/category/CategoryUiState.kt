package com.emm.justchill.hh.category

import com.emm.domain.transaction.TransactionType
import com.emm.justchill.hh.shared.Empty

data class CategoryUiState(
    val name: String = String.Empty,
    val icon: IconCatalog = AppIconCatalog.catalog.first(),
    val categoryType: TransactionType = TransactionType.Income,
    val color: CategoryColor = allColors.first(),
    val isAllFieldValidated: Boolean = false,
)