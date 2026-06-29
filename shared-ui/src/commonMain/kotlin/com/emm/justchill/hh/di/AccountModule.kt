package com.emm.justchill.hh.di

import com.emm.domain.account.CreateAccountUseCase
import com.emm.domain.account.DeleteAccountUseCase
import com.emm.domain.account.FindAccountUseCase
import com.emm.domain.account.UpdateAccountUseCase
import com.emm.justchill.hh.account.AccountsViewModel
import com.emm.justchill.hh.account.AddAccountViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Domain use-case + ViewModel wiring only. The DefaultAccountRepository ->
// AccountRepository bind lives in commonMain hh/di/DataModule.kt (commonMain
// depends on :data since slice H); platform singles live in each platformModule.
val accountModule = module {
    factoryOf(::CreateAccountUseCase)
    factoryOf(::DeleteAccountUseCase)
    factoryOf(::FindAccountUseCase)
    factoryOf(::UpdateAccountUseCase)

    viewModelOf(::AccountsViewModel)
    viewModelOf(::AddAccountViewModel)
}
