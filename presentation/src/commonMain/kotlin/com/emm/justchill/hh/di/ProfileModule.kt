package com.emm.justchill.hh.di

import com.emm.justchill.hh.profile.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val profileModule = module {
    viewModel {
        ProfileViewModel(
            backupRepository = get(),
            importData = get(),
            signOut = get(),
            deleteUserAccount = get(),
            backupController = get(),
            backupVerifier = get(),
            getBackupStaleness = get(),
            logger = get(),
            categoryRepository = get(),
            accountRepository = get(),
            observeSession = get(),
            appVersion = get(named("appVersion")),
            clock = get(),
        )
    }
}
