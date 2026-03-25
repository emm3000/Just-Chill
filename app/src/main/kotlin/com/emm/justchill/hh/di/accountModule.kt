package com.emm.justchill.hh.di

import com.emm.data.account.DefaultAccountRepository
import com.emm.domain.account.CreateAccountUseCase
import com.emm.domain.account.DeleteAccountUseCase
import com.emm.domain.account.FindAccountUseCase
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.AccountUpdateRepository
import com.emm.domain.account.UpdateAccountUseCase
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val accountModule = module {
    factoryOf(::CreateAccountUseCase)
    factoryOf(::DeleteAccountUseCase)
    factoryOf(::FindAccountUseCase)
    factoryOf(::UpdateAccountUseCase)

    factoryOf(::DefaultAccountRepository) {
        bind<AccountRepository>()
        bind<AccountUpdateRepository>()
    }
}
