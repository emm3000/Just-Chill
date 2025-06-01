package com.emm.justchill.hh.category

import com.emm.justchill.hh.shared.Empty

data class CategoryUiState(
    val name: String = String.Empty,
    val isAllFieldValidated: Boolean = false,
)