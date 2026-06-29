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
    // can't resolve by type. clock is omitted so it falls back to its Clock.System default.
    viewModel {
        ProfileViewModel(
            exportData = get(),
            importData = get(),
            signOut = get(),
            deleteUserAccount = get(),
            syncController = get(),
            categoryRepository = get(),
            accountRepository = get(),
            observeSession = get(),
            appVersion = get(named("appVersion")),
        )
    }
}
