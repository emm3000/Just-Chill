package com.emm.justchill.hh.di

import com.emm.data.auth.DefaultAuthRepository
import com.emm.data.auth.DefaultClaimLocalDataRepository
import com.emm.domain.auth.AuthRepository
import com.emm.domain.auth.ClaimLocalDataOnAuthenticationUseCase
import com.emm.domain.auth.ClaimLocalDataRepository
import com.emm.domain.auth.ClaimLocalDataUseCase
import com.emm.domain.auth.DeleteUserAccountUseCase
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SignInUseCase
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.auth.SignUpUseCase
import com.emm.justchill.hh.auth.AuthViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authModule = module {
    factoryOf(::DefaultAuthRepository) { bind<AuthRepository>() }
    // DefaultClaimLocalDataRepository takes EmmDatabaseData — resolved via get() from dbModule.
    factoryOf(::DefaultClaimLocalDataRepository) { bind<ClaimLocalDataRepository>() }

    factoryOf(::ClaimLocalDataUseCase)
    factoryOf(::SignInUseCase)
    factoryOf(::SignUpUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::ObserveSessionUseCase)
    factoryOf(::ClaimLocalDataOnAuthenticationUseCase)
    factoryOf(::DeleteUserAccountUseCase)

    viewModelOf(::AuthViewModel)
}
