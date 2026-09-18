package com.emm.justchill.hh.onboarding

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.core.preferences.AppPreferences
import com.emm.justchill.core.ui.navigation.AppNavigator
import com.emm.justchill.core.ui.navigation.NavHostBindings
import com.emm.justchill.core.ui.navigation.rememberAppNavigator
import com.emm.justchill.hh.shared.ManifestoRoute

fun EntryProviderScope<NavKey>.onboardingEntries(bindings: NavHostBindings, appPreferences: AppPreferences) {
    entry<ManifestoRoute> { key ->
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack, bindings.startTab)
        ManifestoScreen(
            isRevisit = key.isRevisit,
            onStart = {
                if (key.isRevisit) {
                    nav.pop()
                } else {
                    appPreferences.firstLaunchSeen = true
                    nav.replaceAll(bindings.startTab)
                }
            },
        )
    }
}
