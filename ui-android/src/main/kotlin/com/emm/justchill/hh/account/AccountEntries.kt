package com.emm.justchill.hh.account

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
import com.emm.justchill.hh.shared.AccountsRoute
import com.emm.justchill.hh.shared.AddAccountRoute
import com.emm.justchill.hh.shared.LoansRoute
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.accountEntries(bindings: NavHostBindings) {
    entry<AccountsRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack, bindings.startTab)
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
            navigateToLoans = { nav.push(LoansRoute) },
            modifier = Modifier.fillMaxSize(),
        )
    }

    entry<AddAccountRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack, bindings.startTab)
        AddAccountScreen(
            onBack = { nav.pop() },
            snackbarHostState = bindings.snackbarHostState,
        )
    }
}
