package com.emm.justchill.core

import com.emm.justchill.core.domain.shared.logging.DiagnosticsLogger
import com.emm.justchill.core.preferences.AppPreferences
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appScopeQualifier = named("appScope")

val commonCoreModule = module {
    single { AppPreferences(get()) }

    // The handler is a backstop, not the primary defence: BackupOrchestrator catches its own
    // failures. Without it, anything it misses reaches Android's default handler, which crashes
    // the app for a feature it is fully usable without.
    single<CoroutineScope>(appScopeQualifier) {
        val logger = get<DiagnosticsLogger>()
        val handler = CoroutineExceptionHandler { _, throwable ->
            logger.warn("uncaught in appScope", throwable)
        }
        CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)
    }
}
