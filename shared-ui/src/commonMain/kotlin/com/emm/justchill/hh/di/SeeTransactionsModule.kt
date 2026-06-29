package com.emm.justchill.hh.di

import com.emm.justchill.hh.seetransactions.SeeTransactionsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// ViewModel wiring only; its use cases live in transactionModule and resolve
// globally. Repository + data-source binds live in commonMain hh/di/DataModule.kt
// (commonMain depends on :data since slice H).
val seetransactionsModule = module {
    viewModelOf(::SeeTransactionsViewModel)
}
