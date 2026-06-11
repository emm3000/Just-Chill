package com.emm.justchill.hh.di

import com.emm.data.auth.DefaultAuthRepository
import com.emm.data.auth.DefaultClaimLocalDataRepository
import com.emm.domain.auth.AuthRepository
import com.emm.domain.auth.ClaimLocalDataOnAuthenticationUseCase
import com.emm.domain.auth.ClaimLocalDataRepository
import com.emm.domain.auth.ClaimLocalDataUseCase
import com.emm.domain.auth.DeleteUserAccountUseCase
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.ResendConfirmationEmailUseCase
import com.emm.domain.auth.SignInUseCase
import com.emm.domain.auth.SignInWithGoogleUseCase
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.auth.SignUpUseCase
import com.emm.justchill.BuildConfig
import com.emm.justchill.hh.auth.ActivityGoogleSignInLauncher
import com.emm.justchill.hh.auth.AuthViewModel
import com.emm.justchill.hh.auth.GoogleCredentialClient
import com.emm.justchill.hh.auth.GoogleSignInLauncher
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    factoryOf(::DefaultAuthRepository) { bind<AuthRepository>() }
    // DefaultClaimLocalDataRepository takes EmmDatabaseData — resolved via get() from dbModule.
    factoryOf(::DefaultClaimLocalDataRepository) { bind<ClaimLocalDataRepository>() }

    factoryOf(::GoogleCredentialClient)
    factoryOf(::ActivityGoogleSignInLauncher) { bind<GoogleSignInLauncher>() }

    factoryOf(::ClaimLocalDataUseCase)
    factoryOf(::ResendConfirmationEmailUseCase)
    factoryOf(::SignInUseCase)
    factoryOf(::SignInWithGoogleUseCase)
    factoryOf(::SignUpUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::ObserveSessionUseCase)
    factoryOf(::ClaimLocalDataOnAuthenticationUseCase)
    factoryOf(::DeleteUserAccountUseCase)

    viewModel {
        AuthViewModel(
            signIn = get(),
            signUp = get(),
            signInWithGoogle = get(),
            resendConfirmationEmail = get(),
            googleServerClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
            googleSignInLauncher = get(),
        )
    }
}
