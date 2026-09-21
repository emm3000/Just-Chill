package com.emm.justchill.wiring

import com.emm.justchill.core.backup.auth.DefaultAuthRepository
import com.emm.justchill.core.domain.auth.AuthRepository
import com.emm.justchill.core.domain.auth.DeleteUserAccountUseCase
import com.emm.justchill.core.domain.auth.GetSessionStatusUseCase
import com.emm.justchill.core.domain.auth.ResendConfirmationEmailUseCase
import com.emm.justchill.core.domain.auth.SignInUseCase
import com.emm.justchill.core.domain.auth.SignInWithGoogleUseCase
import com.emm.justchill.core.domain.auth.SignOutUseCase
import com.emm.justchill.core.domain.auth.SignUpUseCase
import com.emm.justchill.feature.auth.authModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val authWiring: Module = module {
    includes(authModule)

    factoryOf(::DefaultAuthRepository) { bind<AuthRepository>() }

    factoryOf(::DeleteUserAccountUseCase)
    factoryOf(::GetSessionStatusUseCase)
    factoryOf(::ResendConfirmationEmailUseCase)
    factoryOf(::SignInUseCase)
    factoryOf(::SignInWithGoogleUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::SignUpUseCase)
}
