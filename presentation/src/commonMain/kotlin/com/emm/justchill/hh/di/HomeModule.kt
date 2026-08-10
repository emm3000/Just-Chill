package com.emm.justchill.hh.di

import com.emm.justchill.hh.home.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Home has no exclusive use cases — its dependencies (transaction/recurring use
// cases, Clock) are bound by their own modules and resolve globally at startup.
// Repository + data-source binds live in commonMain hh/di/DataModule.kt
// (commonMain depends on :data since slice H).
val homeModule = module {
    viewModelOf(::HomeViewModel)
}
