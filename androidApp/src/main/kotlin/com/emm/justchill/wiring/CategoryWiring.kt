package com.emm.justchill.wiring

import com.emm.justchill.core.domain.category.CreateCategoryUseCase
import com.emm.justchill.core.domain.category.DeleteCategoryUseCase
import com.emm.justchill.core.domain.category.UpdateCategoryUseCase
import com.emm.justchill.feature.category.categoryModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val categoryWiring: Module = module {

    factoryOf(::CreateCategoryUseCase)
    factoryOf(::DeleteCategoryUseCase)
    factoryOf(::UpdateCategoryUseCase)

    includes(categoryModule)
}
