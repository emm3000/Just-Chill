package com.emm.justchill.hh.di

import com.emm.domain.category.CreateCategoryUseCase
import com.emm.domain.category.DeleteCategoryUseCase
import com.emm.domain.category.FindCategoryUseCase
import com.emm.domain.category.UpdateCategoryUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val categoryModule = module {

    factoryOf(::CreateCategoryUseCase)
    factoryOf(::DeleteCategoryUseCase)
    factoryOf(::UpdateCategoryUseCase)
    factoryOf(::FindCategoryUseCase)
}
