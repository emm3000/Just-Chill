package com.emm.justchill.core

import com.emm.domain.shared.logging.DiagnosticsLogger
import com.emm.justchill.core.preferences.AppPreferences
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Qualifier for the single application-lifetime [CoroutineScope].
 *
 * It lived in `syncModule` until ADR 009 2c-iii-b, and it was never engine property: one scope drives
 * the claim-on-sign-in observer, `SyncOrchestrator`'s loops today and `BackupOrchestrator`'s loops
 * from now on. `docs/sync/ADR009_PLAN.md` Phase 5 deletes `SyncModule.kt` whole, so a backup
 * orchestrator resolving its scope from there would be a survivor wired into a file scheduled for
 * deletion — the same reason `resumeEvents()` and the diagnostics logger moved out ahead of it.
 */
val appScopeQualifier = named("appScope")

// Platform-agnostic core wiring. AppPreferences sits over the platform Settings single
// (SharedPreferencesSettings on Android / NSUserDefaultsSettings on iOS), both provided by the
// platform module. The Settings impl, DispatchersProvider, appVersion, and the Supabase/Google
// platform config all stay platform-side (see androidPlatformModule / iosPlatformModule).
val commonCoreModule = module {
    single { AppPreferences(get()) }

    // Application-lifetime scope for the long-lived jobs of both orchestrators and the claim
    // observer. The handler is a backstop, not the primary defence: every one of those catches its
    // own failures. Without it, anything they miss reaches the default handler, which on Android is
    // a crash — for features the app is fully usable without. The logger is resolved once, up front,
    // so the handler never has to touch Koin while unwinding a failure.
    single<CoroutineScope>(appScopeQualifier) {
        val logger = get<DiagnosticsLogger>()
        val handler = CoroutineExceptionHandler { _, throwable ->
            logger.warn("uncaught in appScope", throwable)
        }
        CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)
    }
}
