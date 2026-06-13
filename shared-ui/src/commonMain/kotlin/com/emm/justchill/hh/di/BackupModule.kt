package com.emm.justchill.hh.di

import com.emm.domain.shared.backup.ExportDataUseCase
import com.emm.domain.shared.backup.ImportDataUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

// Domain use-case wiring only. The DefaultBackupRepository -> BackupRepository
// binding lives in :app HhModule because commonMain depends on :domain only,
// not :data.
val backupModule = module {
    factoryOf(::ExportDataUseCase)
    factoryOf(::ImportDataUseCase)
}
