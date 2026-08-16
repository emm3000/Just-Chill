package com.emm.justchill.hh.di

import com.emm.domain.shared.backup.BackupMetadataStore
import com.emm.domain.shared.backup.GetBackupStalenessUseCase
import com.emm.domain.shared.backup.ImportDataUseCase
import com.emm.justchill.core.appScopeQualifier
import com.emm.justchill.core.backup.BackupController
import com.emm.justchill.core.backup.BackupOrchestrator
import com.emm.justchill.core.backup.DefaultBackupMetadataStore
import com.emm.justchill.core.lifecycle.backgroundEvents
import com.emm.justchill.core.lifecycle.resumeEvents
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.named
import org.koin.dsl.module

val backupModule = module {
    factoryOf(::ImportDataUseCase)

    factoryOf(::GetBackupStalenessUseCase)

    factoryOf(::DefaultBackupMetadataStore) { bind<BackupMetadataStore>() }

    single {
        BackupOrchestrator(
            backupRepository = get(),
            uploader = get(),
            pruner = get(),
            metadata = get(),
            observeSession = get(),
            appVersion = get(named("appVersion")),
            clock = get(),
            timeZone = get(),
            externalScope = get(appScopeQualifier),
            backgroundEvents = backgroundEvents(),
            resumeEvents = resumeEvents(),
            logger = get(),
        )
    } withOptions { bind<BackupController>() }
}
