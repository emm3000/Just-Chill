package com.emm.justchill.core.viewmodel

import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.feature.category.AddCategoryEffect
import com.emm.justchill.feature.category.AddCategoryIntent
import com.emm.justchill.feature.category.AddCategoryUiState
import com.emm.justchill.feature.category.AddCategoryViewModel
import com.emm.justchill.feature.category.CategoriesEffect
import com.emm.justchill.feature.category.CategoriesIntent
import com.emm.justchill.feature.category.CategoriesUiState
import com.emm.justchill.feature.category.CategoriesViewModel
import org.koin.core.parameter.parametersOf

fun categoriesViewModel(): MviHandle<CategoriesUiState, CategoriesIntent, CategoriesEffect> =
    handleOf(CategoriesViewModel::class)

fun addCategoryViewModel(
    initialType: String,
    initialName: String,
): MviHandle<AddCategoryUiState, AddCategoryIntent, AddCategoryEffect> =
    handleOf(AddCategoryViewModel::class) { parametersOf(CategoryType.valueOf(initialType), initialName) }
