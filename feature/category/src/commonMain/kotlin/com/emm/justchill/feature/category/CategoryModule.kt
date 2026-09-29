package com.emm.justchill.feature.category

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val categoryModule: Module = module {

    viewModelOf(::CategoriesViewModel)

    viewModel { parameters ->
        AddCategoryViewModel(
            createCategory = get(),
            initialType = parameters.get(),
            initialName = parameters.get(),
        )
    }
}
