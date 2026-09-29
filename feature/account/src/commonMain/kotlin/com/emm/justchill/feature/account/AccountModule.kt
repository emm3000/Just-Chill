package com.emm.justchill.feature.account

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val accountModule: Module = module {
    viewModelOf(::AccountsViewModel)
    viewModelOf(::AddAccountViewModel)
}
