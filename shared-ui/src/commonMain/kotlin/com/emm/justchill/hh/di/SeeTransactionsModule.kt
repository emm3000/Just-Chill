package com.emm.justchill.hh.di

import com.emm.justchill.hh.seetransactions.SeeTransactionsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// ViewModel wiring only; its use cases live in transactionModule and resolve
// globally. Repository binds + data sources stay in :app HhModule because
// commonMain depends on :domain only, not :data.
val seetransactionsModule = module {
    viewModelOf(::SeeTransactionsViewModel)
}
