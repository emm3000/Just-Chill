package com.emm.justchill.hh.onboarding

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.core.preferences.AppPreferences
import com.emm.justchill.hh.shared.AppNavigator
import com.emm.justchill.hh.shared.ManifestoRoute
import com.emm.justchill.hh.shared.NavHostBindings
import com.emm.justchill.hh.shared.rememberAppNavigator
import com.emm.justchill.hh.shared.startTab

/**
 * Registers the onboarding entries on the host: [ManifestoRoute].
 *
 * @param appPreferences flips `firstLaunchSeen` when the manifesto is completed for the first time,
 *   which is what makes the host's start-route gate land on `startTab` from the next launch on.
 */
fun EntryProviderScope<NavKey>.onboardingEntries(bindings: NavHostBindings, appPreferences: AppPreferences) {
    entry<ManifestoRoute> { key ->
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        ManifestoScreen(
            isRevisit = key.isRevisit,
            onStart = {
                if (key.isRevisit) {
                    nav.pop()
                } else {
                    appPreferences.firstLaunchSeen = true
                    nav.replaceAll(startTab)
                }
            },
        )
    }
}
