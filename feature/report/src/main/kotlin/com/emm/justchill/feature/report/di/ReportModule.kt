package com.emm.justchill.feature.report.di

import com.emm.justchill.feature.report.ReportViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val reportModule: Module = module {
    viewModelOf(::ReportViewModel)
}
