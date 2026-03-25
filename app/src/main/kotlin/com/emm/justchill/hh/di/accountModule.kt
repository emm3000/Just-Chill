package com.emm.justchill.hh.di

import com.emm.data.account.DefaultAccountRepository
import com.emm.domain.account.AccountCreator
import com.emm.domain.account.AccountDeleter
import com.emm.domain.account.AccountFinder
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.AccountUpdateRepository
import com.emm.domain.account.AccountUpdater
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val accountModule = module {
    factoryOf(::AccountCreator)
    factoryOf(::AccountDeleter)
    factoryOf(::AccountFinder)
    factoryOf(::AccountUpdater)

    factoryOf(::DefaultAccountRepository) {
        bind<AccountRepository>()
        bind<AccountUpdateRepository>()
    }
}
