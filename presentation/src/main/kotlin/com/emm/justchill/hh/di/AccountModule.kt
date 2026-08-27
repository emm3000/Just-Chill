package com.emm.justchill.hh.di

import com.emm.domain.account.CreateAccountUseCase
import com.emm.domain.account.DeleteAccountUseCase
import com.emm.domain.account.UpdateAccountUseCase
import com.emm.justchill.hh.account.AccountsViewModel
import com.emm.justchill.hh.account.AddAccountViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val accountModule = module {
    factoryOf(::CreateAccountUseCase)
    factoryOf(::DeleteAccountUseCase)
    factoryOf(::UpdateAccountUseCase)

    viewModelOf(::AccountsViewModel)
    viewModelOf(::AddAccountViewModel)
}
