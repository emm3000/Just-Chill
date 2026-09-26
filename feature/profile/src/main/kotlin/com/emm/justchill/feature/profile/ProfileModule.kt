package com.emm.justchill.feature.profile

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val profileModule: Module = module {
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
            localExportHistory = get(),
            todayFlow = get(),
            categoryRepository = get(),
            getSessionStatus = get(),
            backupAvailability = get(),
            exportTransactionsCsv = get(),
            appVersion = get(named("appVersion")),
            clock = get(),
        )
    }
}
