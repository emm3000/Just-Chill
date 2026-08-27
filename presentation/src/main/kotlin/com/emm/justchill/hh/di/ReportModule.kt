package com.emm.justchill.hh.di

import com.emm.domain.report.GetMonthlyAmountByCategoryUseCase
import com.emm.domain.report.GetMonthlyComparisonUseCase
import com.emm.domain.report.GetMonthlySectionStatsUseCase
import com.emm.domain.report.GetSavingsRateUseCase
import com.emm.domain.report.GetTopCategoriesOverMonthsUseCase
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
