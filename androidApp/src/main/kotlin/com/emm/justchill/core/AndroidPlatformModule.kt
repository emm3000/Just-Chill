package com.emm.justchill.core

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.emm.data.provideDb
import com.emm.data.provideSqlDriver
import com.emm.justchill.BuildConfig
import com.emm.justchill.core.platform.CurrentActivityHolder
import com.emm.justchill.hh.auth.ActivityGoogleSignInLauncher
import com.emm.justchill.hh.auth.GoogleCredentialClient
import com.emm.justchill.hh.auth.GoogleSignInLauncher
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.Settings
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

// Stable prefs file name, independent of OS build. Build.ID changes on every Android OS update
// which caused the app to silently open a new empty file after an update, resetting onboarding
// state and sync cursors. A one-time migration copies existing data from the old Build.ID file.
private const val PREFS_NAME = "justchill_prefs"
private const val PREFS_MIGRATED_FLAG = "_migrated_from_build_id"

// Android platform Koin module — the ONLY place Android-specific DI lives after the commonMain dedup
// (slice H). Supplies every binding whose construction is Android-specific; the platform-agnostic
// graph (feature modules + supabase/sync/auth/data/commonCore wiring) is shared via appModules().
val androidPlatformModule = module {

    // SQLDelight: AndroidSqliteDriver (with its onCreate default-category seed) + EmmDatabaseData.
    single { provideSqlDriver(androidContext()) }
    single { provideDb(get()) }

    single<DispatchersProvider> { DefaultDispatcher() }
    single<Settings> { SharedPreferencesSettings(provideSharedPreferences(androidContext())) }
    single { CurrentActivityHolder() }

    // Platform-provided app version (no BuildConfig in commonMain). Consumed by ProfileViewModel
    // via the "appVersion" qualifier; stamped into exported backups.
    single(named("appVersion")) { BuildConfig.VERSION_NAME }

    // Google Sign-In web client id, consumed by AuthViewModel. Empty when supabase.properties is
    // absent; the Google button stays hidden so submitWithGoogle never reaches the launcher.
    single(named("googleServerClientId")) { BuildConfig.GOOGLE_WEB_CLIENT_ID }

    // Supabase connection settings injected into the commonMain supabaseModule. Empty URL falls back
    // to the localhost placeholder so the app stays usable in anonymous/offline mode.
    single<SupabaseConfig> {
        SupabaseConfig(
            url = BuildConfig.SUPABASE_URL.ifBlank { "http://localhost:54321" },
            anonKey = BuildConfig.SUPABASE_ANON_KEY,
        )
    }

    // Google Sign-In launcher (Android-only; iOS uses the no-op UnavailableGoogleSignInLauncher).
    factoryOf(::GoogleCredentialClient)
    factoryOf(::ActivityGoogleSignInLauncher) { bind<GoogleSignInLauncher>() }
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
    val editor = target.edit()
    for ((key, value) in legacy.all) {
        when (value) {
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is String -> editor.putString(key, value)
            is Set<*> -> @Suppress("UNCHECKED_CAST") editor.putStringSet(key, value as Set<String>)
        }
    }
    editor.putBoolean(PREFS_MIGRATED_FLAG, true)
    editor.apply()
}
