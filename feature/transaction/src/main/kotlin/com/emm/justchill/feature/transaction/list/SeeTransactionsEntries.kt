package com.emm.justchill.feature.transaction.list

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.core.ui.navigation.AppNavigator
import com.emm.justchill.core.ui.navigation.NavHostBindings
import com.emm.justchill.core.ui.navigation.rememberAppNavigator
import com.emm.justchill.feature.transaction.AddTransactionRoute
import com.emm.justchill.feature.transaction.EditTransactionRoute
import com.emm.justchill.feature.transaction.SeeTransactionRoute
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.seeTransactionsEntries(bindings: NavHostBindings) {
    entry<SeeTransactionRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        val vm: SeeTransactionsViewModel = koinViewModel()

        LaunchedEffect(vm) {
            vm.effect.collect { effect ->
                when (effect) {
                    is SeeTransactionsEffect.ShowError -> bindings.snackbarHostState.showEmmSnackbar(
                        message = effect.message,
                        tone = EmmSnackbarTone.Error,
                    )
                }
            }
        }

        SeeTransactionsScreen(
            onEditTransaction = { id ->
                nav.push(EditTransactionRoute(id))
            },
            onAddTransaction = { nav.push(AddTransactionRoute()) },
            vm = vm,
        )
    }
}
