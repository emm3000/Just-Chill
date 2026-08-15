package com.emm.justchill.hh.di

import com.emm.domain.shared.backup.BackupMetadataStore
import com.emm.domain.shared.backup.ImportDataUseCase
import com.emm.justchill.core.backup.DefaultBackupMetadataStore
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

// Domain use-case wiring only. The DefaultBackupRepository -> BackupRepository
// bind lives in commonMain hh/di/DataModule.kt (commonMain depends on :data
// since slice H).
val backupModule = module {
    factoryOf(::ImportDataUseCase)

    // Domain port: the backup watermark seam (ADR 009 2c-iii-a), bound in the backup feature's own
    // module — NOT syncModule, which Phase 5 deletes outright.
    factoryOf(::DefaultBackupMetadataStore) { bind<BackupMetadataStore>() }
}
