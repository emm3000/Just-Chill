package com.emm.justchill.hh.di

import com.emm.domain.recurring.ConfirmRecurringMovementUseCase
import com.emm.domain.recurring.CreateRecurringMovementUseCase
import com.emm.domain.recurring.DeleteRecurringMovementUseCase
import com.emm.domain.recurring.GetPendingRecurringMovementsUseCase
import com.emm.domain.recurring.GetRecurringMonthlyTotalsUseCase
import com.emm.domain.recurring.SkipRecurringMovementUseCase
import com.emm.domain.recurring.UpdateRecurringMovementUseCase
import com.emm.justchill.hh.recurring.AddEditRecurringMovementViewModel
import com.emm.justchill.hh.recurring.RecurringMovementsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Domain use-case + ViewModel wiring only. Repository + data-source binds live
// in commonMain hh/di/DataModule.kt (commonMain depends on :data since slice H).
val recurringModule = module {
    factoryOf(::GetPendingRecurringMovementsUseCase)
    factoryOf(::ConfirmRecurringMovementUseCase)
    factoryOf(::SkipRecurringMovementUseCase)
    factoryOf(::GetRecurringMonthlyTotalsUseCase)
    factoryOf(::CreateRecurringMovementUseCase)
    factoryOf(::UpdateRecurringMovementUseCase)
    factoryOf(::DeleteRecurringMovementUseCase)

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
