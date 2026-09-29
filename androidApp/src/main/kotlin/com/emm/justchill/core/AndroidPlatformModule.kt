package com.emm.justchill.core

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import com.emm.justchill.BuildConfig
import com.emm.justchill.BuildInfo
import com.emm.justchill.core.auth.ActivityGoogleSignInLauncher
import com.emm.justchill.core.auth.GoogleCredentialClient
import com.emm.justchill.core.backup.FlavorBackupAvailability
import com.emm.justchill.core.database.provideDb
import com.emm.justchill.core.database.provideSqlDriver
import com.emm.justchill.core.domain.shared.backup.BackupAvailability
import com.emm.justchill.core.domain.shared.logging.DiagnosticsLogger
import com.emm.justchill.core.platform.CurrentActivityHolder
import com.emm.justchill.core.session.KeystoreSessionCipher
import com.emm.justchill.core.session.KeystoreSessionManager
import com.emm.justchill.core.shortcuts.ShortcutPublisher
import com.emm.justchill.feature.auth.GoogleSignInLauncher
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.jan.supabase.auth.SessionManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

// A fixed name, never Build.ID: Build.ID changes on every OS update, and a new name opens an
// empty file that drops every stored preference.
private const val PREFS_NAME = "justchill_prefs"
internal const val AUTH_PREFS_NAME = "justchill_auth"
private const val PREFS_MIGRATED_FLAG = "_migrated_from_build_id"

val androidPlatformModule = module {

    single { provideSqlDriver(androidContext()) }
    single { provideDb(get()) }

    single<Settings> { SharedPreferencesSettings(provideSharedPreferences(androidContext())) }

    // A file of its own so the extraction rules can exclude the refresh token under a name no
    // applicationId suffix can move; supabase-kt would otherwise default it into
    // "<applicationId>_preferences", which device-to-device migration still copies.
    single {
        KeystoreSessionManager(
            prefs = androidContext().getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE),
            cipher = KeystoreSessionCipher(),
            diagnostics = get(),
        )
    }

    single<SessionManager> { get<KeystoreSessionManager>() }
    single { CurrentActivityHolder() }

    single<DiagnosticsLogger> { CrashReportingDiagnosticsLogger() }

    single(named("appVersion")) { BuildConfig.VERSION_NAME }

    single { CommitHash(BuildInfo.commitHash) }

    single(named("googleServerClientId")) { BuildConfig.GOOGLE_WEB_CLIENT_ID }

    single<SupabaseConfig> {
        SupabaseConfig.withOfflineFallback(url = BuildConfig.SUPABASE_URL, anonKey = BuildConfig.SUPABASE_ANON_KEY)
    }

    singleOf(::FlavorBackupAvailability) { bind<BackupAvailability>() }

    factoryOf(::GoogleCredentialClient)
    factoryOf(::ActivityGoogleSignInLauncher) { bind<GoogleSignInLauncher>() }

    single { ShortcutPublisher(androidContext(), get()) }
}

private fun provideSharedPreferences(context: Context): SharedPreferences {
    val stable = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (!stable.getBoolean(PREFS_MIGRATED_FLAG, false)) {
        val legacy = context.getSharedPreferences(Build.ID, Context.MODE_PRIVATE)
        migrateBuildIdPrefs(legacy, stable)
    }
    return stable
}

internal fun migrateBuildIdPrefs(legacy: SharedPreferences, target: SharedPreferences) {
    target.edit {
        for ((key, value) in legacy.all) {
            when (value) {
                is Boolean -> putBoolean(key, value)

                is Int -> putInt(key, value)

                is Long -> putLong(key, value)

                is Float -> putFloat(key, value)

                is String -> putString(key, value)

                is Set<*> ->
                    @Suppress("UNCHECKED_CAST")
                    putStringSet(key, value as Set<String>)
            }
        }
        putBoolean(PREFS_MIGRATED_FLAG, true)
    }
}
