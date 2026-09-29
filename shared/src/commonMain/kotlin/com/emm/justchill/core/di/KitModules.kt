package com.emm.justchill.core.di

import com.emm.justchill.core.commonCoreModule
import org.koin.core.module.Module

val kitModules: List<Module> = listOf(
    backupModule,
    sharedModule,
    supabaseModule,
    dataModule,
    commonCoreModule,
)
