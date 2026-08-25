package com.emm.justchill.core

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import com.emm.data.provideDb
import com.emm.data.provideSqlDriver
import com.emm.domain.shared.logging.DiagnosticsLogger
import com.emm.justchill.BuildConfig
import com.emm.justchill.BuildInfo
import com.emm.justchill.core.platform.CurrentActivityHolder
import com.emm.justchill.core.session.KeystoreSessionCipher
import com.emm.justchill.core.session.KeystoreSessionManager
import com.emm.justchill.core.shortcuts.ShortcutPublisher
import com.emm.justchill.hh.auth.ActivityGoogleSignInLauncher
import com.emm.justchill.hh.auth.GoogleCredentialClient
import com.emm.justchill.hh.auth.GoogleSignInLauncher
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.jan.supabase.auth.SessionManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

// Stable prefs file name, independent of OS build. Build.ID changes on every Android OS update
// which caused the app to silently open a new empty file after an update, resetting onboarding
// state and sync cursors. A one-time migration copies existing data from the old Build.ID file.
private const val PREFS_NAME = "justchill_prefs"
internal const val AUTH_PREFS_NAME = "justchill_auth"
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

    // Bound by concrete type as well, because EmmApp sweeps the legacy cleartext key at launch and
    // sweepLegacySession() is not part of the SessionManager port.
    single<SessionManager> { get<KeystoreSessionManager>() }
    single { CurrentActivityHolder() }

    // Sync observability sink. Platform-specific because it reports to Crashlytics (Android-only);
    // iOS binds a println implementation of the same port in KoinIos.kt.
    single<DiagnosticsLogger> { CrashReportingDiagnosticsLogger() }

    // Platform-provided app version (no BuildConfig in commonMain). Consumed by ProfileViewModel
    // via the "appVersion" qualifier; stamped into exported backups.
    single(named("appVersion")) { BuildConfig.VERSION_NAME }

    // The git commit this APK was built from, FULL 40-char sha. Consumed by AppNavHost, which hands
    // it to the profile footer; the footer shows the first 7 and copies all 40.
    //
    // Bound by TYPE, not under a qualifier: producer and consumer are in different Gradle modules,
    // and CommitHash (:presentation) is the declaration both import. See its KDoc.
    //
    // Deliberately not in testPlatformModule / KoinIos: its only consumer is :ui-android's Compose
    // host, which is Android-only and outside appModules(), so AppGraphKoinTest would be asserting
    // wiring no shared consumer resolves — the same reason DispatchersProvider is absent there.
    // AndroidPlatformModuleTest is what guards this line instead: deleting it turns that test red.
    single { CommitHash(BuildInfo.commitHash) }

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

    // Launcher shortcuts: ShortcutManagerCompat needs an Android Context, so the publisher built
    // from :presentation's GetSpendShortcutCombos lives here rather than beside it (E09-03).
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
