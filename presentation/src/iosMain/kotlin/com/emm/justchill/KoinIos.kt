package com.emm.justchill

import com.emm.data.provideDb
import com.emm.data.provideSqlDriver
import com.emm.data.seedDefaultCategoriesIfEmpty
import com.emm.domain.sync.SyncLogger
import com.emm.justchill.core.SupabaseConfig
import com.emm.justchill.core.appModules
import com.emm.justchill.core.bootstrapAppGraph
import com.emm.justchill.hh.auth.GoogleSignInLauncher
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

// iOS platform Koin module — the iOS analogue of androidPlatformModule, and the ONLY place iOS-specific
// DI lives after the commonMain dedup (slice H). Supplies every binding whose construction is
// iOS-specific: the SQLDelight native driver + DB (with explicit default-category seeding, since the
// native driver has no onCreate hook), Settings over NSUserDefaults, the app version, an empty Google
// web client id, the SupabaseConfig from the generated IosSupabaseConfig, and the no-op Google sign-in
// launcher. Everything else is shared via appModules().
//
// DispatchersProvider is intentionally NOT bound here: its only consumer is the Android-only dev-flavor
// ExperiencesLocalDataSource. No commonMain or iOS code resolves it, so binding it on iOS would be dead
// wiring (the previous iOS module set didn't bind it either). If a commonMain consumer of
// DispatchersProvider is ever added, an iOS actual (or a commonMain default) MUST be bound here.
private val iosPlatformModule = module {
    // SQLDelight: NativeSqliteDriver + EmmDatabaseData. The native driver has no onCreate hook, so the
    // Android driver's onCreate default-category seed is reproduced here, guarded idempotently.
    single {
        val db = provideDb(provideSqlDriver())
        seedDefaultCategoriesIfEmpty(db)
        db
    }

    single<Settings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }

    // Sync observability sink. Android reports to Crashlytics; iOS has no crash-reporting SDK
    // wired (ADR 003), so the console is the whole sink here.
    single<SyncLogger> { PrintlnSyncLogger() }

    // App version surfaced in the Profile footer (no BuildConfig on iOS).
    single(named("appVersion")) { "1.0.0" }

    // No Google web client id on iOS — Google Sign-In is deferred; AuthViewModel.submitWithGoogle
    // short-circuits on a blank id (and the button is hidden anyway).
    single(named("googleServerClientId")) { "" }

    // Supabase connection settings from the generated IosSupabaseConfig (build/, from root
    // supabase.properties). IosSupabaseConfig already applies the empty -> localhost fallback at
    // generation time, so no .ifBlank here (parity with Android's SupabaseConfig construction).
    single<SupabaseConfig> {
        SupabaseConfig(
            url = IosSupabaseConfig.SUPABASE_URL,
            anonKey = IosSupabaseConfig.SUPABASE_ANON_KEY,
        )
    }

    // Google Sign-In is deferred on iOS: the launcher is the no-op UnavailableGoogleSignInLauncher.
    factoryOf(::UnavailableGoogleSignInLauncher) { bind<GoogleSignInLauncher>() }
}

// Called once from Swift at app launch (iOSApp.init). Swift sees this top-level fn as
// KoinIosKt.doInitKoin() (the `init` prefix is mangled by the Kotlin/Native Obj-C exporter).
fun initKoin() {
    val koinApp = startKoin {
        modules(appModules(iosPlatformModule))
    }
    bootstrapAppGraph(koinApp.koin)
}
