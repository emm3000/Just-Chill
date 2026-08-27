package com.emm.justchill.core

import com.emm.domain.shared.logging.DiagnosticsLogger
import com.emm.justchill.core.preferences.AppPreferences
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Qualifier for the single application-lifetime [CoroutineScope] that drives `BackupOrchestrator`'s loops. */
val appScopeQualifier = named("appScope")

// Platform-agnostic core wiring. AppPreferences sits over the platform Settings single
// (SharedPreferencesSettings), provided by the platform module. The Settings impl,
// DispatchersProvider, appVersion, and the Supabase/Google platform config all stay platform-side
// (see androidPlatformModule).
val commonCoreModule = module {
    single { AppPreferences(get()) }

    // Application-lifetime scope for BackupOrchestrator's long-lived jobs. The handler is a
    // backstop, not the primary defence: BackupOrchestrator catches its own failures. Without it,
    // anything it misses reaches the default handler, which on Android is a crash — for a feature
    // the app is fully usable without. The logger is resolved once, up front, so the handler never
    // has to touch Koin while unwinding a failure.
    single<CoroutineScope>(appScopeQualifier) {
        val logger = get<DiagnosticsLogger>()
        val handler = CoroutineExceptionHandler { _, throwable ->
            logger.warn("uncaught in appScope", throwable)
        }
        CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)
    }
}
