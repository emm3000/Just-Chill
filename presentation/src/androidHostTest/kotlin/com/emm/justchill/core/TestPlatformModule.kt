package com.emm.justchill.core

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import com.emm.data.provideDb
import com.emm.domain.sync.SyncLogger
import com.emm.justchill.hh.auth.GoogleSignInLauncher
import com.emm.justchill.hh.auth.GoogleSignInResult
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.onClose

/**
 * JVM stand-in for `androidPlatformModule` / `iosPlatformModule` — the `platformModule` seam that
 * `appModules(platformModule)` is parameterized by.
 *
 * It supplies the SAME binding set the two real platform modules do, so the rest of the graph
 * (every feature module plus the supabase/sync/auth/data/commonCore wiring) can be built and
 * resolved off-device by [AppGraphKoinTest]. Construction is REAL wherever that is cheap — a real
 * SQLDelight schema on an in-memory JDBC driver, a real Settings store, a real Supabase client —
 * because the whole point is proving the production graph actually wires up, not that mocks do.
 *
 * ONE binding is deliberately absent: `DispatchersProvider`. `androidPlatformModule` binds it, but
 * its only consumer is the Android dev-flavor `experiencesModule`, which is appended by `:androidApp`
 * and is not part of `appModules()`. `iosPlatformModule` omits it for the same reason, so binding it
 * here would assert wiring that no shared consumer resolves.
 *
 * [CommitHash] is absent for the same reason. It is resolved by `AppNavHost` in `:ui-android`,
 * outside `appModules()`, so binding it here would have asserted nothing about the production
 * binding in `androidPlatformModule` — a module this source set cannot even import. That binding is
 * guarded by `AndroidPlatformModuleTest` in `:androidApp`, where it lives.
 */
val testPlatformModule: Module = module {

    // Real SQLDelight schema over an in-memory JDBC database (the driver :data's own host tests use).
    // The Android driver seeds default categories from its onCreate callback and iOS calls
    // seedDefaultCategoriesIfEmpty; neither is reproduced here because DI resolution never reads rows.
    single<SqlDriver> {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        driver
    } onClose { it?.close() }
    single { provideDb(get()) }

    // In-memory Settings (multiplatform-settings-test), replacing SharedPreferencesSettings /
    // NSUserDefaultsSettings. AppPreferences sits on top of it in commonCoreModule.
    single<Settings> { MapSettings() }

    // Stands in for CrashReportingSyncLogger / PrintlnSyncLogger. The graph resolves it eagerly
    // (the appScope single reads it to build its CoroutineExceptionHandler), so it must be bound
    // here even though this test never logs anything.
    single<SyncLogger> { NoOpSyncLogger() }

    // Stamped into exported backups and shown in the Profile footer; a literal is enough off-device.
    single(named("appVersion")) { "0.0.0-test" }

    // Blank, exactly like iOS: AuthViewModel hides the Google button and short-circuits
    // submitWithGoogle on a blank id, so the launcher below is never actually invoked.
    single(named("googleServerClientId")) { "" }

    // The offline/anonymous fallback both platforms apply when supabase.properties is absent.
    // createSupabaseClient requires a non-blank URL but performs no network I/O at construction.
    single<SupabaseConfig> {
        SupabaseConfig(
            url = "http://localhost:54321",
            anonKey = "",
        )
    }

    factoryOf(::NoOpGoogleSignInLauncher) { bind<GoogleSignInLauncher>() }
}

/**
 * No-op launcher mirroring iOS's `UnavailableGoogleSignInLauncher` (which lives in iosMain and is
 * therefore unreachable from here). Never invoked: the test only constructs the graph.
 */
private class NoOpGoogleSignInLauncher : GoogleSignInLauncher {
    override suspend fun signIn(serverClientId: String): GoogleSignInResult =
        GoogleSignInResult.Failure(IllegalStateException("Google Sign-In is not available in tests"))
}

/** Discards everything: this test asserts wiring, and a real sink would only add console noise. */
private class NoOpSyncLogger : SyncLogger {
    override fun warn(message: String, throwable: Throwable?) = Unit
}
