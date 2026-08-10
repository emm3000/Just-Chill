package com.emm.justchill.core

import com.emm.justchill.core.preferences.AppPreferences
import org.koin.dsl.module

// Platform-agnostic core wiring. AppPreferences sits over the platform Settings single
// (SharedPreferencesSettings on Android / NSUserDefaultsSettings on iOS), both provided by the
// platform module. The Settings impl, DispatchersProvider, appVersion, and the Supabase/Google
// platform config all stay platform-side (see androidPlatformModule / iosPlatformModule).
val commonCoreModule = module {
    single { AppPreferences(get()) }
}
