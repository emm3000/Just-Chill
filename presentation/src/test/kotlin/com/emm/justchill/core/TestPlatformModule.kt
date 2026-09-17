package com.emm.justchill.core

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import com.emm.data.provideDb
import com.emm.justchill.core.domain.shared.logging.DiagnosticsLogger
import com.emm.justchill.hh.auth.GoogleSignInLauncher
import com.emm.justchill.hh.auth.GoogleSignInResult
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.SettingsSessionManager
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.onClose

// JVM stand-in for androidPlatformModule. Construction is REAL wherever cheap, because the point is
// proving the production graph actually wires up, not that mocks do. DispatchersProvider and
// CommitHash are absent: neither is part of appModules(), both guarded by AndroidPlatformModuleTest.
val testPlatformModule: Module = module {

    // The Android driver seeds default categories from its onCreate callback; that is not
    // reproduced here because DI resolution never reads rows.
    single<SqlDriver> {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        driver
    } onClose { it?.close() }
    single { provideDb(get()) }

    // In-memory Settings (multiplatform-settings-test), replacing SharedPreferencesSettings.
    // AppPreferences sits on top of it in commonCoreModule.
    single<Settings> { MapSettings() }

    single<SessionManager> { SettingsSessionManager(MapSettings()) }

    // Stands in for CrashReportingDiagnosticsLogger / PrintlnDiagnosticsLogger. The graph resolves it eagerly
    // (the appScope single reads it to build its CoroutineExceptionHandler), so it must be bound
    // here even though this test never logs anything.
    single<DiagnosticsLogger> { NoOpDiagnosticsLogger() }

    // Stamped into exported backups and shown in the Profile footer; a literal is enough off-device.
    single(named("appVersion")) { "0.0.0-test" }

    // Blank: AuthViewModel hides the Google button and short-circuits submitWithGoogle on a blank
    // id, so the launcher below is never actually invoked.
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

// AuthViewModel's Koin dependency must resolve to something, and no real launcher works
// off-device. Never invoked: the test only constructs the graph.
private class NoOpGoogleSignInLauncher : GoogleSignInLauncher {
    override suspend fun signIn(serverClientId: String): GoogleSignInResult =
        GoogleSignInResult.Failure(IllegalStateException("Google Sign-In is not available in tests"))
}

private class NoOpDiagnosticsLogger : DiagnosticsLogger {
    override fun warn(message: String, throwable: Throwable?) = Unit
}
