package com.emm.justchill.hh.di

import com.emm.justchill.core.domain.report.GetMonthlyAmountByCategoryUseCase
import com.emm.justchill.core.domain.report.GetMonthlyComparisonUseCase
import com.emm.justchill.core.domain.report.GetMonthlySectionStatsUseCase
import com.emm.justchill.core.domain.report.GetSavingsRateUseCase
import com.emm.justchill.core.domain.report.GetTopCategoriesOverMonthsUseCase
import com.emm.justchill.hh.report.ReportViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val reportModule = module {
    factoryOf(::GetMonthlyAmountByCategoryUseCase)
    factoryOf(::GetMonthlyComparisonUseCase)
    factoryOf(::GetMonthlySectionStatsUseCase)
    factoryOf(::GetSavingsRateUseCase)
    factoryOf(::GetTopCategoriesOverMonthsUseCase)

    viewModelOf(::ReportViewModel)
}
