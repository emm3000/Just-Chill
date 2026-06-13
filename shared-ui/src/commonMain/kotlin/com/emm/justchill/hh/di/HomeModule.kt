package com.emm.justchill.hh.di

import com.emm.justchill.hh.home.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Home has no exclusive use cases — its dependencies (transaction/recurring use
// cases, Clock) are bound by their own modules and resolve globally at startup.
// Repository binds + data sources stay in :app HhModule because commonMain
// depends on :domain only, not :data.
val homeModule = module {
    viewModelOf(::HomeViewModel)
}
