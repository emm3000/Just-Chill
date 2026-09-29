package com.emm.justchill.core

import com.emm.justchill.core.database.provideDb
import com.emm.justchill.core.database.provideSqlDriver
import com.emm.justchill.core.domain.shared.backup.BackupAvailability
import com.emm.justchill.core.domain.shared.logging.DiagnosticsLogger
import com.emm.justchill.feature.auth.GoogleSignInLauncher
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.SettingsSessionManager
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

private const val AUTH_KEYCHAIN_SERVICE: String = "com.emm.justchill.auth"

fun iosPlatformModule(config: KitConfig): Module = module {
    single { provideSqlDriver() }
    single { provideDb(get()) }

    single<Settings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }

    // The refresh token goes to the Keychain: supabase-kt would default it into NSUserDefaults, a
    // plain plist that device backups carry.
    @OptIn(ExperimentalSettingsImplementation::class)
    single<SessionManager> { SettingsSessionManager(KeychainSettings(AUTH_KEYCHAIN_SERVICE)) }

    single<DiagnosticsLogger> { NSLogDiagnosticsLogger() }

    single(named("appVersion")) { config.appVersion }

    single(named("googleServerClientId")) { config.googleServerClientId }

    factory<GoogleSignInLauncher> { NoCredentialsGoogleSignInLauncher() }

    single { SupabaseConfig.withOfflineFallback(url = config.supabaseUrl, anonKey = config.supabaseAnonKey) }

    single<BackupAvailability> { FixedBackupAvailability(config.isSnapshotBackupEnabled) }
}
