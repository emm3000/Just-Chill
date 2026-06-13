package com.emm.justchill.hh.di

import com.emm.domain.account.CreateAccountUseCase
import com.emm.domain.account.DeleteAccountUseCase
import com.emm.domain.account.FindAccountUseCase
import com.emm.domain.account.UpdateAccountUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

// Domain use-case wiring only. The DefaultAccountRepository -> AccountRepository
// binding lives in :app HhModule.repositoriesProviders() because commonMain
// depends on :domain only, not :data.
val accountModule = module {
    factoryOf(::CreateAccountUseCase)
    factoryOf(::DeleteAccountUseCase)
    factoryOf(::FindAccountUseCase)
    factoryOf(::UpdateAccountUseCase)
}
