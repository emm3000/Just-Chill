package com.emm.justchill.core

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.justchill.core.database.JustChillDatabase
import com.emm.justchill.core.database.provideDb
import com.emm.justchill.core.domain.shared.backup.BackupAvailability
import com.emm.justchill.core.domain.shared.logging.DiagnosticsLogger
import com.emm.justchill.core.testing.FakeBackupAvailability
import com.emm.justchill.core.testing.NoOpDiagnosticsLogger
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.SettingsSessionManager
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.onClose

val kitTestPlatformModule: Module = module {

    single<SqlDriver> {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        JustChillDatabase.Schema.create(driver)
        driver
    } onClose { it?.close() }
    single { provideDb(get()) }

    single<Settings> { MapSettings() }

    single<SessionManager> { SettingsSessionManager(MapSettings()) }

    single<DiagnosticsLogger> { NoOpDiagnosticsLogger() }

    single(named("appVersion")) { "0.0.0-test" }

    single { SupabaseConfig.withOfflineFallback(url = "", anonKey = "") }

    single<BackupAvailability> { FakeBackupAvailability(isAvailable = true) }
}
