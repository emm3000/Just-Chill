package com.emm.justchill.wiring

import com.emm.justchill.core.domain.account.CreateAccountUseCase
import com.emm.justchill.core.domain.account.DeleteAccountUseCase
import com.emm.justchill.core.domain.account.UpdateAccountUseCase
import com.emm.justchill.feature.account.accountModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val accountWiring: Module = module {
    includes(accountModule)

    factoryOf(::CreateAccountUseCase)
    factoryOf(::DeleteAccountUseCase)
    factoryOf(::UpdateAccountUseCase)
}
