package com.emm.justchill.hh.di

import com.emm.domain.category.CreateCategoryUseCase
import com.emm.domain.category.DeleteCategoryUseCase
import com.emm.domain.category.FindCategoryUseCase
import com.emm.domain.category.UpdateCategoryUseCase
import com.emm.justchill.hh.category.AddCategoryViewModel
import com.emm.justchill.hh.category.CategoriesViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val categoryModule = module {

    factoryOf(::CreateCategoryUseCase)
    factoryOf(::DeleteCategoryUseCase)
    factoryOf(::UpdateCategoryUseCase)
    factoryOf(::FindCategoryUseCase)

    viewModelOf(::CategoriesViewModel)

    viewModel { parameters ->
        AddCategoryViewModel(
            createCategory = get(),
            initialType = parameters.get(),
            initialName = parameters.get(),
        )
    }
}
