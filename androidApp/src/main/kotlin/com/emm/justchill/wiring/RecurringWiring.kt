package com.emm.justchill.wiring

import com.emm.justchill.core.domain.recurring.ConfirmRecurringMovementUseCase
import com.emm.justchill.core.domain.recurring.CreateRecurringMovementUseCase
import com.emm.justchill.core.domain.recurring.DeleteRecurringMovementUseCase
import com.emm.justchill.core.domain.recurring.GetPendingRecurringMovementsUseCase
import com.emm.justchill.core.domain.recurring.GetRecurringMonthlySummaryUseCase
import com.emm.justchill.core.domain.recurring.GetRecurringMonthlyTotalsUseCase
import com.emm.justchill.core.domain.recurring.SkipRecurringMovementUseCase
import com.emm.justchill.core.domain.recurring.UpdateRecurringMovementUseCase
import com.emm.justchill.feature.recurring.recurringModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val recurringWiring: Module = module {
    includes(recurringModule)

    factoryOf(::GetPendingRecurringMovementsUseCase)
    factoryOf(::ConfirmRecurringMovementUseCase)
    factoryOf(::SkipRecurringMovementUseCase)
    factoryOf(::GetRecurringMonthlyTotalsUseCase)
    factoryOf(::GetRecurringMonthlySummaryUseCase)
    factoryOf(::CreateRecurringMovementUseCase)
    factoryOf(::UpdateRecurringMovementUseCase)
    factoryOf(::DeleteRecurringMovementUseCase)
}
