package com.emm.justchill.hh.di

import com.emm.domain.account.AccountBalanceUpdater
import com.emm.domain.account.AccountCreator
import com.emm.domain.account.AccountDeleter
import com.emm.domain.account.AccountFinder
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.AccountUpdater
import com.emm.domain.account.DailyAccountCreator
import com.emm.data.account.DefaultAccountRepository
import com.emm.domain.account.AccountUpdateRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.binds
import org.koin.dsl.module

val accountModule = module {

    factoryOf(::AccountCreator)
    factoryOf(::DailyAccountCreator)
    factoryOf(::AccountDeleter)
    factoryOf(::AccountFinder)
    factoryOf(::AccountUpdater)

    factory {
        DefaultAccountRepository(
            emmDatabase = get(),
            uniqueIdProvider = get(),
        )
    } binds arrayOf(AccountRepository::class, AccountUpdateRepository::class)

    factoryOf(::AccountBalanceUpdater)
}