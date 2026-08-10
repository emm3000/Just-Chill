package com.emm.justchill.hh.auth

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.hh.shared.AppNavigator
import com.emm.justchill.hh.shared.AuthRoute
import com.emm.justchill.hh.shared.NavHostBindings
import com.emm.justchill.hh.shared.rememberAppNavigator

/** Registers the auth entries on the host: [AuthRoute]. */
fun EntryProviderScope<NavKey>.authEntries(bindings: NavHostBindings) {
    entry<AuthRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        AuthScreen(
            onBack = { nav.pop() },
            snackbarHostState = bindings.snackbarHostState,
            onOpenEmailApp = bindings.platform.onOpenEmailApp,
            showGoogleSignIn = bindings.platform.showGoogleSignIn,
        )
    }
}
