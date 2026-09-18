package com.emm.justchill.hh.auth

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.core.ui.navigation.AppNavigator
import com.emm.justchill.core.ui.navigation.NavHostBindings
import com.emm.justchill.core.ui.navigation.rememberAppNavigator
import com.emm.justchill.hh.shared.AuthRoute

fun EntryProviderScope<NavKey>.authEntries(bindings: NavHostBindings) {
    entry<AuthRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack, bindings.startTab)
        AuthScreen(
            onBack = { nav.pop() },
            snackbarHostState = bindings.snackbarHostState,
            onOpenEmailApp = bindings.platform.onOpenEmailApp,
            showGoogleSignIn = bindings.platform.showGoogleSignIn,
        )
    }
}
