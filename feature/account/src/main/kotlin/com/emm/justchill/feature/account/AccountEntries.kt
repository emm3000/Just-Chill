package com.emm.justchill.feature.account

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.core.ui.navigation.AppNavigator
import com.emm.justchill.core.ui.navigation.NavHostBindings
import com.emm.justchill.core.ui.navigation.rememberAppNavigator
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.accountEntries(
    bindings: NavHostBindings,
    onOpenLoans: (AppNavigator) -> Unit,
) {
    entry<AccountsRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        val vm: AccountsViewModel = koinViewModel()
        val accountsState by vm.state.collectAsStateWithLifecycle()

        LaunchedEffect(vm) {
            vm.effect.collect { effect ->
                when (effect) {
                    is AccountsEffect.ShowMessage -> bindings.showMessage(effect.text)
                }
            }
        }

        AccountsScreen(
            state = accountsState,
            onIntent = vm::onIntent,
            addAccount = { nav.push(AddAccountRoute) },
            navigateToLoans = { onOpenLoans(nav) },
            onBack = { nav.pop() },
            modifier = Modifier.fillMaxSize(),
        )
    }

    entry<AddAccountRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        AddAccountScreen(
            onBack = { nav.pop() },
            snackbarHostState = bindings.snackbarHostState,
        )
    }
}
