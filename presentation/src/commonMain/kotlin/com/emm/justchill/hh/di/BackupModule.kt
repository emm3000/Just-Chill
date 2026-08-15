package com.emm.justchill.hh.di

import com.emm.domain.shared.backup.BackupMetadataStore
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

// Domain use-case wiring only. The DefaultBackupRepository -> BackupRepository
// bind lives in commonMain hh/di/DataModule.kt (commonMain depends on :data
// since slice H).
val backupModule = module {
    factoryOf(::ImportDataUseCase)

    // Domain port: the backup watermark seam (ADR 009 2c-iii-a), bound in the backup feature's own
    // module — NOT syncModule, which Phase 5 deletes outright.
    factoryOf(::DefaultBackupMetadataStore) { bind<BackupMetadataStore>() }

    // Single: owns the long-lived trigger job and the one request channel that serialises snapshots,
    // so a second instance would be a second concurrency guard guarding nothing. Does NOT
    // self-start — bootstrapAppGraph calls start() behind SNAPSHOT_BACKUP_ENABLED.
    //
    // Bound HERE and not in syncModule for the same reason as the store above: Phase 5 deletes that
    // module, and a missing Koin binding is a runtime crash no build gate can see. AppGraphKoinTest
    // is the net, and it covers this definition twice over — once by resolving it, once by
    // asserting the Clock and TimeZone below are the instances the graph bound rather than defaults
    // this block invented (docs/DATE_AUDIT.md #7, why neither parameter has a default any more).
    //
    // Hand-written rather than singleOf(::BackupOrchestrator): appVersion is a qualified String the
    // constructor DSL cannot resolve by type, and the two lifecycle triggers are expect/actual
    // top-level functions rather than graph types.
    //
    // `withOptions { bind<BackupController>() }` adds the port as a SECONDARY type of this same
    // definition, so ProfileViewModel and bootstrapAppGraph resolve one instance. A second `single`
    // for the port would compile, resolve and pass every resolution sweep in the suite while handing
    // the UI a controller whose request channel nothing drains — `the backup controller port is the
    // orchestrator single, not a second instance` in AppGraphKoinTest is what makes that red.
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
