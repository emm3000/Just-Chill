package com.emm.justchill.feature.auth

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val authModule: Module = module {
    viewModel {
        AuthViewModel(
            signIn = get(),
            signUp = get(),
            signInWithGoogle = get(),
            resendConfirmationEmail = get(),
            googleServerClientId = get(named("googleServerClientId")),
            googleSignInLauncher = get<GoogleSignInLauncher>(),
        )
    }
}
