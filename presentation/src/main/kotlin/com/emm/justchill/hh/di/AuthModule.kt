package com.emm.justchill.hh.di

import com.emm.justchill.core.backup.auth.DefaultAuthRepository
import com.emm.justchill.core.domain.auth.AuthRepository
import com.emm.justchill.core.domain.auth.DeleteUserAccountUseCase
import com.emm.justchill.core.domain.auth.ObserveSessionUseCase
import com.emm.justchill.core.domain.auth.ResendConfirmationEmailUseCase
import com.emm.justchill.core.domain.auth.SignInUseCase
import com.emm.justchill.core.domain.auth.SignInWithGoogleUseCase
import com.emm.justchill.core.domain.auth.SignOutUseCase
import com.emm.justchill.core.domain.auth.SignUpUseCase
import com.emm.justchill.hh.auth.AuthViewModel
import com.emm.justchill.hh.auth.GoogleSignInLauncher
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

// DefaultAuthRepository takes SupabaseClient (supabaseModule). The Google sign-in launcher and the
// googleServerClientId string are platform-provided by androidPlatformModule
// (ActivityGoogleSignInLauncher + BuildConfig).
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
