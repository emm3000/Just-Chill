package com.emm.justchill.feature.onboarding

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.core.ui.navigation.AppNavigator
import com.emm.justchill.core.ui.navigation.NavHostBindings
import com.emm.justchill.core.ui.navigation.rememberAppNavigator

fun EntryProviderScope<NavKey>.onboardingEntries(bindings: NavHostBindings, onFirstLaunchSeen: () -> Unit) {
    entry<ManifestoRoute> { key ->
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack, bindings.startTab)
        ManifestoScreen(
            isRevisit = key.isRevisit,
            onStart = {
                if (key.isRevisit) {
                    nav.pop()
                } else {
                    onFirstLaunchSeen()
                    nav.replaceAll(bindings.startTab)
                }
            },
        )
    }
}
