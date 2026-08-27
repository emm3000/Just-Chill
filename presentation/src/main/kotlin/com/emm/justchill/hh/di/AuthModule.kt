package com.emm.justchill.hh.di

import com.emm.data.auth.DefaultAuthRepository
import com.emm.domain.auth.AuthRepository
import com.emm.domain.auth.DeleteUserAccountUseCase
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.ResendConfirmationEmailUseCase
import com.emm.domain.auth.SignInUseCase
import com.emm.domain.auth.SignInWithGoogleUseCase
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.auth.SignUpUseCase
import com.emm.justchill.hh.auth.AuthViewModel
import com.emm.justchill.hh.auth.GoogleSignInLauncher
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

// Single commonMain auth wiring (replaces :androidApp/hh/di/AuthModule.kt). DefaultAuthRepository
// takes SupabaseClient (supabaseModule). The Google sign-in launcher and the googleServerClientId
// string are platform-provided by androidPlatformModule (ActivityGoogleSignInLauncher + BuildConfig).
val authModule = module {
    factoryOf(::DefaultAuthRepository) { bind<AuthRepository>() }

    factoryOf(::ResendConfirmationEmailUseCase)
    factoryOf(::SignInUseCase)
    factoryOf(::SignInWithGoogleUseCase)
    factoryOf(::SignUpUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::ObserveSessionUseCase)
    factoryOf(::DeleteUserAccountUseCase)

    viewModel {
        AuthViewModel(
            signIn = get(),
            signUp = get(),
            signInWithGoogle = get(),
            resendConfirmationEmail = get(),
            // Platform-provided: androidPlatformModule's BuildConfig.GOOGLE_WEB_CLIENT_ID.
            googleServerClientId = get(named("googleServerClientId")),
            googleSignInLauncher = get<GoogleSignInLauncher>(),
        )
    }
}
