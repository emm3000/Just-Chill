package com.emm.justchill.core

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.emm.justchill.BuildConfig
import com.emm.justchill.core.DispatchersProvider
import com.emm.justchill.core.platform.CurrentActivityHolder
import com.emm.justchill.core.preferences.AppPreferences
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.Settings
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

// Stable prefs file name, independent of OS build. Build.ID changes on every Android OS update
// which caused the app to silently open a new empty file after an update, resetting onboarding
// state and sync cursors. A one-time migration copies existing data from the old Build.ID file.
private const val PREFS_NAME = "justchill_prefs"
private const val PREFS_MIGRATED_FLAG = "_migrated_from_build_id"

val coreModule = module {

    single<DispatchersProvider> { DefaultDispatcher() }
    single<Settings> { SharedPreferencesSettings(provideSharedPreferences(androidContext())) }
    single { AppPreferences(get()) }
    single { CurrentActivityHolder() }

    // Platform-provided app version (no BuildConfig in commonMain). Consumed by ProfileViewModel
    // via the "appVersion" qualifier; stamped into exported backups.
    single(named("appVersion")) { BuildConfig.VERSION_NAME }
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
