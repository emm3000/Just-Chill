package com.emm.justchill.feature.recurring

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val recurringModule: Module = module {
    viewModelOf(::RecurringMovementsViewModel)

    viewModel { parameters ->
        AddEditRecurringMovementViewModel(
            id = parameters.getOrNull(),
            accountRepository = get(),
            categoryRepository = get(),
            recurringRepository = get(),
            createRecurring = get(),
            updateRecurring = get(),
        )
    }
}
