package com.emm.justchill.wiring

import com.emm.justchill.core.domain.report.GetMonthlyAmountByCategoryUseCase
import com.emm.justchill.core.domain.report.GetMonthlyComparisonUseCase
import com.emm.justchill.core.domain.report.GetMonthlySectionStatsUseCase
import com.emm.justchill.core.domain.report.GetSavingsRateUseCase
import com.emm.justchill.core.domain.report.GetTopCategoriesOverMonthsUseCase
import com.emm.justchill.feature.report.di.reportModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val reportWiring: Module = module {
    includes(reportModule)

    factoryOf(::GetMonthlyAmountByCategoryUseCase)
    factoryOf(::GetMonthlyComparisonUseCase)
    factoryOf(::GetMonthlySectionStatsUseCase)
    factoryOf(::GetSavingsRateUseCase)
    factoryOf(::GetTopCategoriesOverMonthsUseCase)
}
