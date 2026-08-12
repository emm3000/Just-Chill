package com.emm.justchill.hh.di

import com.emm.justchill.hh.profile.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

// ViewModel wiring only. Repository binds + data sources live in the commonMain dataModule
// (slice H). The "appVersion" qualifier is provided by the platform module
// (androidPlatformModule / iosPlatformModule).
val profileModule = module {
    // Explicit block (not viewModelOf): appVersion is a qualified String the constructor-DSL
    // can't resolve by type. That is a reason to name every argument here, not a reason to leave
    // one out. The clock used to be omitted, and it was the only place in the app where a Kotlin
    // default was actually evaluated at runtime — harmlessly, since it was Clock.System and that
    // is exactly what sharedModule binds. The reason it had to go is drift: rebind Clock there and
    // this one consumer would have kept reading Clock.System, silently. Now it cannot compile, and
    // AppGraphKoinTest fails if a default ever comes back and lets it.
    viewModel {
        ProfileViewModel(
            backupRepository = get(),
            importData = get(),
            signOut = get(),
            deleteUserAccount = get(),
            syncController = get(),
            categoryRepository = get(),
            accountRepository = get(),
            observeSession = get(),
            appVersion = get(named("appVersion")),
            clock = get(),
        )
    }
}
