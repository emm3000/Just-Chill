package com.emm.justchill.hh.category

import com.emm.domain.transaction.TransactionType
import com.emm.justchill.hh.shared.Empty

data class CategoryUiState(
    val name: String = String.Empty,
    val description: String = String.Empty,
    val transactionType: TransactionType = TransactionType.Income,
    val isAllFieldValidated: Boolean = false,
)